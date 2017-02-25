package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.PluginManager;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.List;

class OptionsDialog extends JFrame {
	
	private final MainWindow mainWindow;
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final Action openSkinManagerAction = new AbstractAction("...") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			SkinManagerDialog dialog = new SkinManagerDialog(mainWindow, OptionsDialog.this);
			dialog.setVisible(true);
			openSkinManagerAction.putValue(Action.NAME, mainWindow.getCharacterWidget().getCurrentSkin().getName());
		}
	};
	private JList pluginsList;
	private final Action loadPluginAction = new AbstractAction(MainWindow.getString("load")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
			chooser.setDialogTitle(MainWindow.getString("load_plugin"));
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setAcceptAllFileFilterUsed(false);
			if (chooser.showOpenDialog(getContentPane()) == JFileChooser.APPROVE_OPTION) {
				Path path = chooser.getSelectedFile().toPath();
				try {
					PluginManager.getInstance().loadPluginByPath(path);
				} catch (Throwable e) {
					MainWindow.showThrowable(OptionsDialog.this, e);
				}
			}
		}
	};
	private final Action unloadPluginAction = new AbstractAction(MainWindow.getString("unload")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			String plugin = pluginsList.getSelectedValue().toString();
			if ((plugin != null) && !plugin.equals("core")) {
				PluginManager.getInstance().unloadPlugin(plugin);
			}
		}
	};
	private DefaultMutableTreeNode alternativesTreeRoot;
	private JTree alternativesTree;
	private JTextField debugTagTextField;
	private JTextArea debugDataTextArea;
	private final Action debugSendMessageAction = new AbstractAction(MainWindow.getString("send")) {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			String tag = debugTagTextField.getText();
			String dataStr = debugDataTextArea.getText();
			try {
				JSONObject json = new JSONObject(dataStr);
				Object data = json.toMap();
				mainWindow.getPluginProxy().sendMessage(tag, data);
			} catch (Throwable e) {
				MainWindow.showThrowable(OptionsDialog.this, e);
			}
		}
	};
	
	OptionsDialog(MainWindow mainWindow) {
		super(MainWindow.getString("deskchan_options"));
		this.mainWindow = mainWindow;
		setMinimumSize(new Dimension(600, 300));
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setContentPane(tabbedPane);
		JPanel appearanceTab = new JPanel(new BorderLayout());
		JPanel panel = new JPanel(new GridLayout(2, 2));
		panel.add(new JLabel(MainWindow.getString("look_and_feel")));
		panel.add(new LookAndFeelComboBox());
		panel.add(new JLabel(MainWindow.getString("skin")));
		panel.add(new JButton(openSkinManagerAction));
		appearanceTab.add(panel, BorderLayout.PAGE_START);
		tabbedPane.addTab(MainWindow.getString("appearance"), appearanceTab);
		JPanel pluginsTab = new JPanel(new BorderLayout());
		DefaultListModel pluginsListModel = new DefaultListModel();
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-load", (sender, tag, data) -> {
			MainWindow.runOnEventThread(() -> {
				pluginsListModel.addElement(data.toString());
			});
		});
		mainWindow.getPluginProxy().addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			MainWindow.runOnEventThread(() -> {
				for (int i = 0; i < pluginsListModel.size(); ++i) {
					if (pluginsListModel.elementAt(i).equals(data)) {
						pluginsListModel.remove(i);
						break;
					}
				}
			});
		});
		pluginsList = new JList(pluginsListModel);
		JScrollPane pluginsListScrollPane = new JScrollPane(pluginsList);
		pluginsTab.add(pluginsListScrollPane);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(new JButton(loadPluginAction));
		buttonPanel.add(new JButton(unloadPluginAction));
		pluginsTab.add(buttonPanel, BorderLayout.PAGE_END);
		tabbedPane.addTab(MainWindow.getString("plugins"), pluginsTab);
		JPanel alternativesTab = new JPanel(new BorderLayout());
		alternativesTreeRoot = new DefaultMutableTreeNode("Alternatives");
		alternativesTree = new JTree(alternativesTreeRoot);
		alternativesTree.setRootVisible(false);
		JScrollPane alternativesScrollPane = new JScrollPane(alternativesTree);
		alternativesTab.add(alternativesScrollPane);
		tabbedPane.addTab(MainWindow.getString("alternatives"), alternativesTab);
		JPanel debugTab = new JPanel(new BorderLayout());
		debugTagTextField = new JTextField("DeskChan:say");
		debugTab.add(debugTagTextField, BorderLayout.PAGE_START);
		debugDataTextArea = new JTextArea("{\n\t\"text\": \"Test\"\n}\n");
		JScrollPane debugDataTextAreaScrollPane = new JScrollPane(debugDataTextArea);
		debugTab.add(debugDataTextAreaScrollPane);
		debugTab.add(new JButton(debugSendMessageAction), BorderLayout.PAGE_END);
		tabbedPane.addTab(MainWindow.getString("debug"), debugTab);
		pack();
	}
	
	void updateOptions() {
		openSkinManagerAction.putValue(Action.NAME, mainWindow.getCharacterWidget().getCurrentSkin().getName());
		mainWindow.getPluginProxy().sendMessage("core:query-alternatives-map", null, (sender, data) -> {
			alternativesTreeRoot.removeAllChildren();
			Map<String, Object> m = (Map<String, Object>) (((Map) data).get("map"));
			for (Map.Entry<String, Object> entry : m.entrySet()) {
				DefaultMutableTreeNode alternativeRoot = new DefaultMutableTreeNode(entry.getKey());
				for (Map<String, Object> info : (List<Map<String, Object>>) (entry.getValue())) {
					String tag = info.get("tag").toString();
					String plugin = info.get("plugin").toString();
					int priority = (int) info.get("priority");
					String text = tag + " (plugin: " + plugin + ", priority: " + String.valueOf(priority) + ")";
					DefaultMutableTreeNode alternativeNode = new DefaultMutableTreeNode(text);
					alternativeRoot.add(alternativeNode);
				}
				alternativesTreeRoot.add(alternativeRoot);
			}
			alternativesTree.expandPath(new TreePath(alternativesTreeRoot.getPath()));
		});
	}
	
}
