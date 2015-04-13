/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Chandan R. Rupakheti (chandan.rupakheti@rose-hulman.edu)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.rosehulman.p2p.app;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import edu.rosehulman.p2p.impl.ConnectionMonitor;
import edu.rosehulman.p2p.impl.Host;
import edu.rosehulman.p2p.impl.notification.IActivityListener;
import edu.rosehulman.p2p.impl.notification.IConnectionListener;
import edu.rosehulman.p2p.impl.notification.IDownloadListener;
import edu.rosehulman.p2p.impl.notification.IFindListener;
import edu.rosehulman.p2p.impl.notification.IFindProgressListener;
import edu.rosehulman.p2p.impl.notification.IListingListener;
import edu.rosehulman.p2p.impl.notification.IRequestLogListener;
import edu.rosehulman.p2p.protocol.IHost;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;

/**
 * @author rupakhet
 *
 */
public class P2PGUI implements IActivityListener, IConnectionListener,
		IDownloadListener, IListingListener, IRequestLogListener,
		IFindListener, IFindProgressListener {
	JFrame frame;
	JPanel contentPane;

	JPanel newConnectionPanel;
	JTextField hostNameField;
	JTextField portField;
	JButton connectButton;

	JPanel peersPanel;
	JScrollPane peerListScrollPane;
	JList<IHost> peerList;
	DefaultListModel<IHost> peerListModel;
	JButton disconnectButton;
	JButton listFileButton;
	JScrollPane fileListingPane;
	JList<String> fileList;
	DefaultListModel<String> fileListModel;
	JButton downloadDirect;

	JPanel statusPanel;
	JScrollPane statusScrollPane;
	JTextArea statusTextArea;
	JScrollPane requestLogScrollPane;
	DefaultListModel<String> requestLogListModel;
	JList<String> requestLogList;

	JPanel searchFilePanel;
	JTextField searchTermField;
	JButton searchButton;
	JList<String> searchResultList;
	DefaultListModel<String> searchResultListModel;
	JScrollPane searchResultScrollPane;
	JButton downloadAfterSearch;

	JPanel networkMapPanel;

	IP2PMediator mediator;
	ConnectionMonitor connectionMonitor;

	public P2PGUI(IP2PMediator mediator, ConnectionMonitor connectionMonitor) {
		this.mediator = mediator;
		this.connectionMonitor = connectionMonitor;
		this.initGUI();
	}

	public void show() {
		if (frame.isVisible())
			return;

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				connectionMonitor.stop();
			}
		});

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private void initGUI() {
		frame = new JFrame("Rose P2P App (" + IProtocol.PROTOCOL
				+ ") - Localhost [" + mediator.getLocalHost() + "]");
		this.contentPane = (JPanel) frame.getContentPane();

		this.configurePeersPanel();
		this.createNetworkMapPanel();
		this.createSearchPanel();
		this.createStatusPanel();

		this.contentPane.add(this.peersPanel, BorderLayout.WEST);
		this.contentPane.add(this.networkMapPanel, BorderLayout.CENTER);
		this.contentPane.add(this.searchFilePanel, BorderLayout.EAST);
		this.contentPane.add(this.statusPanel, BorderLayout.SOUTH);
	}

	private void configurePeersPanel() {
		this.peersPanel = new JPanel(new BorderLayout());
		this.peersPanel.setBorder(BorderFactory
				.createTitledBorder("Remote Connections"));

		this.newConnectionPanel = new JPanel();

		this.hostNameField = new JTextField("");
		this.hostNameField.setColumns(25);

		this.portField = new JTextField("");
		this.portField.setColumns(8);

		this.connectButton = new JButton("Connect");
		this.newConnectionPanel.add(new JLabel("Host: "));
		this.newConnectionPanel.add(this.hostNameField);
		this.newConnectionPanel.add(new JLabel("Port: "));
		this.newConnectionPanel.add(this.portField);
		this.newConnectionPanel.add(this.connectButton);

		this.connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String host = hostNameField.getText();
					int port = Integer.parseInt(portField.getText());
					final IHost remoteHost = new Host(host, port);

					Thread runner = new Thread() {
						public void run() {
							postStatus("Trying to connect to " + remoteHost
									+ " ...");
							try {
								if (mediator.requestAttach(remoteHost)) {
									postStatus("Connected to " + remoteHost);
								} else {
									postStatus("Could not connect to "
											+ remoteHost
											+ ". Please try again!");
								}
							} catch (Exception exp) {
								postStatus("An error occured while connecting: "
										+ exp.getMessage());
							}
						}
					};
					runner.start();
				} catch (Exception ex) {
					postStatus("Connection could not be established: "
							+ ex.getMessage());
				}
			}
		});

		this.peersPanel.add(this.newConnectionPanel, BorderLayout.NORTH);

		JPanel peerListPanel = new JPanel(new BorderLayout());
		peerListPanel.add(new JLabel("List of Peers", JLabel.CENTER),
				BorderLayout.NORTH);
		this.peerListModel = new DefaultListModel<>();
		this.peerList = new JList<>(this.peerListModel);
		this.peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.peerListScrollPane = new JScrollPane(this.peerList);
		this.listFileButton = new JButton("List Files");
		this.disconnectButton = new JButton("Disconnect");
		peerListPanel.add(this.peerListScrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout());
		buttonPanel.add(disconnectButton);
		buttonPanel.add(listFileButton);
		peerListPanel.add(buttonPanel, BorderLayout.SOUTH);

		this.disconnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				IHost remoteHost = peerList.getSelectedValue();
				if (remoteHost == null) {
					JOptionPane
							.showMessageDialog(
									frame,
									"You must first select a peer from the list above!",
									"P2P Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					mediator.requestDetach(remoteHost);
					postStatus("Disconnected from " + remoteHost + "!");
				} catch (Exception ex) {
					postStatus("Error disconnecting to " + remoteHost + "!");
				}
			}
		});

		this.listFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final IHost remoteHost = peerList.getSelectedValue();
				if (remoteHost == null) {
					JOptionPane
							.showMessageDialog(
									frame,
									"You must first select a peer from the list above!",
									"P2P Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				Thread thread = new Thread() {
					public void run() {
						try {
							mediator.requestList(remoteHost);
							postStatus("File listing request sent to "
									+ remoteHost + "!");
						} catch (Exception e) {
							postStatus("Error sending list request to "
									+ remoteHost + "!");
						}
					}
				};
				thread.start();
			}
		});

		JPanel fileListPanel = new JPanel(new BorderLayout());
		fileListPanel.add(new JLabel("List of files in the selected peer",
				JLabel.CENTER), BorderLayout.NORTH);
		this.fileListModel = new DefaultListModel<>();
		this.fileList = new JList<>(this.fileListModel);
		this.fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.fileListingPane = new JScrollPane(this.fileList);
		this.downloadDirect = new JButton("Download the selected file");
		fileListPanel.add(this.fileListingPane, BorderLayout.CENTER);
		fileListPanel.add(this.downloadDirect, BorderLayout.SOUTH);

		this.downloadDirect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final IHost remoteHost = peerList.getSelectedValue();
				final String fileName = fileList.getSelectedValue();
				if (remoteHost == null || fileName == null) {
					JOptionPane
							.showMessageDialog(
									frame,
									"You must have a peer and a file selected from the lists above!",
									"P2P Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				Thread thread = new Thread() {
					public void run() {
						try {
							mediator.requestGet(remoteHost, fileName);
							postStatus("Getting file " + fileName + " from "
									+ remoteHost + "...");
						} catch (Exception e) {
							postStatus("Error sending the get file request to "
									+ remoteHost + "!");
						}
					}
				};
				thread.start();
			}
		});

		this.peersPanel.add(peerListPanel, BorderLayout.WEST);
		this.peersPanel.add(fileListPanel, BorderLayout.CENTER);
	}

	private void createStatusPanel() {
		this.statusPanel = new JPanel(new BorderLayout());
		this.statusPanel
				.setBorder(BorderFactory.createTitledBorder("Activity"));

		JPanel panel = new JPanel(new BorderLayout());
		this.statusTextArea = new JTextArea("");
		this.statusTextArea.setRows(10);
		this.statusScrollPane = new JScrollPane(this.statusTextArea);
		panel.add(new JLabel("Activity Log", JLabel.CENTER), BorderLayout.NORTH);
		panel.add(this.statusScrollPane, BorderLayout.CENTER);
		this.statusPanel.add(panel, BorderLayout.CENTER);

		panel = new JPanel(new BorderLayout());
		this.requestLogListModel = new DefaultListModel<>();
		this.requestLogList = new JList<>(this.requestLogListModel);
		this.requestLogScrollPane = new JScrollPane(this.requestLogList);

		panel.add(new JLabel("Request Log", JLabel.CENTER), BorderLayout.NORTH);
		panel.add(this.requestLogScrollPane, BorderLayout.CENTER);
		this.statusPanel.add(panel, BorderLayout.EAST);
	}

	private void createNetworkMapPanel() {
		this.networkMapPanel = new JPanel(new BorderLayout());
		this.networkMapPanel.setBorder(BorderFactory
				.createTitledBorder("Network Graph"));

		this.networkMapPanel.add(new JLabel(
				"TODO: Need to graph the network ..."));
	}

	private void createSearchPanel() {
		this.searchFilePanel = new JPanel(new BorderLayout());
		this.searchFilePanel.setBorder(BorderFactory
				.createTitledBorder("Network File Searching"));

		JPanel top = new JPanel();
		top.add(new JLabel("Search Term: "));
		this.searchTermField = new JTextField("");
		this.searchTermField.setColumns(15);
		this.searchButton = new JButton("Search Network");
		top.add(this.searchTermField);
		top.add(this.searchButton);

		this.searchResultListModel = new DefaultListModel<>();
		this.searchResultList = new JList<>(this.searchResultListModel);
		this.searchResultScrollPane = new JScrollPane(this.searchResultList);

		this.downloadAfterSearch = new JButton("Download the selected file");

		this.searchFilePanel.add(top, BorderLayout.NORTH);
		this.searchFilePanel.add(this.searchResultScrollPane,
				BorderLayout.CENTER);
		this.searchFilePanel.add(this.downloadAfterSearch, BorderLayout.SOUTH);
	}

	@Override
	public void requestLogChanged(Collection<IPacket> packets) {
		this.requestLogListModel.clear();
		int i = 0;
		for (IPacket p : packets) {
			this.requestLogListModel.addElement(++i + " : " + p.getCommand()
					+ " => " + p.getObject());
		}
	}

	@Override
	public void listingReceived(IHost host, List<String> listing) {
		this.postStatus("File listing received from " + host + "!");
		this.fileListModel.clear();
		for (String f : listing) {
			this.fileListModel.addElement(f);
		}
	}

	@Override
	public void downloadComplete(IHost host, String file) {
		this.postStatus("Download of " + file + " from " + host + " complete!");
	}

	@Override
	public void connectionEstablished(IHost host) {
		this.peerListModel.addElement(host);
	}

	@Override
	public void connectionTerminated(IHost host) {
		this.peerListModel.removeElement(host);
	}

	@Override
	public void activityPerformed(String message, IPacket p) {
		this.postStatus(message + p.getCommand());
	}

	private void postStatus(String msg) {
		this.statusTextArea.append(msg + IProtocol.LF);
		this.statusTextArea.setCaretPosition(this.statusTextArea.getDocument()
				.getLength());
	}

	@Override
	public void filesFound(IHost host, List<String> fileNames) {
		this.postStatus("FIND Partial Result: received files from "
				+ host.getHostAddress());
		this.postStatus("Files: " + fileNames);
	}

	@Override
	public void findProgressChanged(int progressPercent) {
		// TODO Auto-generated method stub
		
	}
}
