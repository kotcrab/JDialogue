/*******************************************************************************
 * Copyright 2013 Pawel Pastuszak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package pl.kotcrab.dialoguelib.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import pl.kotcrab.dialoguelib.editor.components.ComponentTableModel;
import pl.kotcrab.dialoguelib.editor.components.DComponentType;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

public class Editor extends JFrame
{
	private static final long serialVersionUID = 7300428735708717490L;
	private static JFrame window;
	
	private JPanel contentPane;
	private LwjglCanvas canvas;
	private Renderer renderer;
	private JSplitPane rendererSplitPane;
	private PropertyTable table;
	private JTable projectTable;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		App.parseArguments(args);
		
		try
		{
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e)
		{
			e.printStackTrace();
		}
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				window = new Editor();
				window.setVisible(true);
				
				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				GraphicsDevice[] gd = ge.getScreenDevices();
				if(App.getScreenId() > -1 && App.getScreenId() < gd.length)
				{
					window.setLocation(gd[App.getScreenId()].getDefaultConfiguration().getBounds().x, window.getY());
				}
				else if(gd.length > 0)
				{
					window.setLocation(gd[0].getDefaultConfiguration().getBounds().x, window.getY());
				}
				else
				{
					throw new RuntimeException("No Screens Found");
				}
				window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);
				
			}
		});
	}
	
	/**
	 * Create the frame.
	 */
	public Editor()
	{
		super("Dialogue Editor");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 700, 500);
		
		createMenuBar();
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		// toolbar
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setBorder(new LineBorder(new Color(0, 0, 0), 0));
		contentPane.add(toolBar, BorderLayout.NORTH);
		
		JButton saveBtn = new JButton("Save");
		toolBar.add(saveBtn);
		// toolbar end
		
		rendererSplitPane = new JSplitPane();
		rendererSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		rendererSplitPane.setResizeWeight(0.8);
		rendererSplitPane.setContinuousLayout(true);
		contentPane.add(rendererSplitPane, BorderLayout.CENTER);
		
		// popup menu
		final PopupMenu popupMenu = createPopupMenu();
		
		// renderer
		renderer = new Renderer(new EditorListener()
		{
			@Override
			public void mouseRightClicked(int x, int y)
			{
				popupMenu.show(canvas.getCanvas(), x, y);
			}
			
			@Override
			public void showMsg(final String msg, final String title, final int msgType)
			{
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						JOptionPane.showMessageDialog(Editor.window, msg, title, msgType);
					}
				});
			}
			
			@Override
			public void changePropertyTableModel(ComponentTableModel tableModel)
			{
				if(tableModel == null) // if componet doesn't have table model set default
					table.setModel(new DefaultTableModel());
				else
				{
					table.setModel(tableModel);
					ColumnsAutoSizer.sizeColumnsToFit(table);
				}
			}
		});
		
		canvas = new LwjglCanvas(renderer, true);
		canvas.getCanvas().add(popupMenu);
		rendererSplitPane.setLeftComponent(canvas.getCanvas());
		// renderer end
		
		// property table
		JSplitPane propertiesSplitPane = new JSplitPane();
		propertiesSplitPane.setResizeWeight(0.7);
		rendererSplitPane.setRightComponent(propertiesSplitPane);
		
		JPanel propertyPanel = new JPanel();
		propertiesSplitPane.setLeftComponent(propertyPanel);
		propertyPanel.setLayout(new BorderLayout());
		
		table = new PropertyTable(new DefaultTableModel());
		
		propertyPanel.add(table.getTableHeader(), BorderLayout.PAGE_START);
		propertyPanel.add(table, BorderLayout.CENTER);
		// propertytable end
		
		projectTable = new JTable();
		propertiesSplitPane.setRightComponent(projectTable);
		// propertiesSplitPane.setLeftComponent(table);
	}
	
	@Override
	public void dispose()
	{
		renderer.dispose(); // we have to manulay dispose renderer from this thread, or we will get "No OpenGL context found in the current thread."
		super.dispose();
	}
	
	private void createMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		
		JMenu rendererMenu = new JMenu("Renderer");
		menuBar.add(rendererMenu);
		
		JCheckBoxMenuItem chckRenderDebug = new JCheckBoxMenuItem("Render debug info");
		rendererMenu.add(chckRenderDebug);
		
		JCheckBoxMenuItem chckRenderCurves = new JCheckBoxMenuItem("Render Curves");
		chckRenderCurves.setSelected(true);
		rendererMenu.add(chckRenderCurves);
		
		JMenuItem chckResetCamera = new JMenuItem("Reset camera");
		chckResetCamera.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				renderer.resetCamera();
			}
		});
		rendererMenu.add(chckResetCamera);
	}
	
	public PopupMenu createPopupMenu()
	{
		PopupMenu popupMenu = new PopupMenu();
		
		MenuItem mAddText = new MenuItem("Add 'Text'");
		MenuItem mAddChoice = new MenuItem("Add 'Choice'");
		MenuItem mAddRandom = new MenuItem("Add 'Random'");
		MenuItem mAddCallback = new MenuItem("Add 'Callback'");
		MenuItem mAddRelay = new MenuItem("Add 'Relay'");
		MenuItem mAddEnd = new MenuItem("Add 'End'");
		
		mAddText.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				renderer.addComponent(DComponentType.TEXT);
			}
		});
		
		mAddChoice.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				renderer.addComponent(DComponentType.CHOICE);
			}
		});
		
		mAddRandom.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				renderer.addComponent(DComponentType.RANDOM);
			}
		});
		
		mAddCallback.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				renderer.addComponent(DComponentType.CALLBACK);
			}
		});
		
		mAddRelay.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				renderer.addComponent(DComponentType.RELAY);
			}
		});
		
		mAddEnd.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				renderer.addComponent(DComponentType.END);
			}
		});
		
		popupMenu.add(mAddText);
		popupMenu.add(mAddChoice);
		popupMenu.add(mAddRandom);
		popupMenu.add(mAddCallback);
		popupMenu.add(mAddRelay);
		popupMenu.add(mAddEnd);
		
		return popupMenu;
	}
}
