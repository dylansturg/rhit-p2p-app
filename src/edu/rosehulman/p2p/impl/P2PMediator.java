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

package edu.rosehulman.p2p.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.rosehulman.p2p.impl.notification.IActivityListener;
import edu.rosehulman.p2p.impl.notification.IConnectionListener;
import edu.rosehulman.p2p.impl.notification.IDownloadListener;
import edu.rosehulman.p2p.impl.notification.IFindListener;
import edu.rosehulman.p2p.impl.notification.IFindProgressListener;
import edu.rosehulman.p2p.impl.notification.IListingListener;
import edu.rosehulman.p2p.impl.notification.IRequestLogListener;
import edu.rosehulman.p2p.protocol.FileSearch;
import edu.rosehulman.p2p.protocol.IHost;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;
import edu.rosehulman.p2p.protocol.IStreamMonitor;
import edu.rosehulman.p2p.protocol.P2PException;

public class P2PMediator implements IP2PMediator {
	private static final int PROGRESS_COMPLETE = 100;

	Host localhost;
	Map<IHost, IStreamMonitor> hostToInStreamMonitor;
	Map<Integer, IPacket> requestLog;
	Map<String, FileSearch> activeSearches;
	String rootDirectory;

	List<IDownloadListener> downloadListeners;
	List<IListingListener> listingListeners;
	List<IRequestLogListener> requestLogListeners;
	List<IConnectionListener> connectionListeners;
	List<IActivityListener> activityListeners;
	List<IFindListener> findListeners;
	List<IFindProgressListener> findProgressListeners;

	int sequence;

	public P2PMediator(int port, String rootDirectory)
			throws UnknownHostException {
		this.rootDirectory = rootDirectory;

		this.localhost = new Host(InetAddress.getLocalHost().getHostAddress(),
				port);
		this.hostToInStreamMonitor = Collections
				.synchronizedMap(new HashMap<IHost, IStreamMonitor>());

		this.requestLog = Collections
				.synchronizedMap(new HashMap<Integer, IPacket>());

		this.activeSearches = Collections
				.synchronizedMap(new HashMap<String, FileSearch>());

		this.downloadListeners = Collections
				.synchronizedList(new ArrayList<IDownloadListener>());
		this.listingListeners = Collections
				.synchronizedList(new ArrayList<IListingListener>());
		this.requestLogListeners = Collections
				.synchronizedList(new ArrayList<IRequestLogListener>());
		this.connectionListeners = Collections
				.synchronizedList(new ArrayList<IConnectionListener>());
		this.activityListeners = Collections
				.synchronizedList(new ArrayList<IActivityListener>());
		this.findListeners = Collections
				.synchronizedList(new ArrayList<IFindListener>());
		this.findProgressListeners = Collections
				.synchronizedList(new ArrayList<IFindProgressListener>());

		this.sequence = 0;
	}

	public synchronized int newSequenceNumber() {
		return ++this.sequence;
	}

	@Override
	public Host getLocalHost() {
		return this.localhost;
	}

	@Override
	public String getRootDirectory() {
		return this.rootDirectory;
	}

	public void setConnected(IHost host, IStreamMonitor monitor) {
		this.hostToInStreamMonitor.put(host, monitor);
		this.fireConnected(host);
	}

	@Override
	public boolean requestAttach(IHost remoteHost) throws P2PException {
		synchronized (this.hostToInStreamMonitor) {
			if (this.hostToInStreamMonitor.containsKey(remoteHost))
				return false;

			IPacket sPacket = new Packet(IProtocol.PROTOCOL, IProtocol.ATTACH,
					remoteHost.toString());
			sPacket.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
			sPacket.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
			int seqNum = this.newSequenceNumber();
			sPacket.setHeader(IProtocol.SEQ_NUM, seqNum + "");

			try {
				this.logRequest(seqNum, sPacket);

				Socket socket = new Socket(remoteHost.getHostAddress(),
						remoteHost.getPort());
				sPacket.toStream(socket.getOutputStream());

				IPacket rPacket = new Packet();
				rPacket.fromStream(socket.getInputStream());
				if (rPacket.getCommand().equals(IProtocol.ATTACH_OK)) {
					// Connection accepted
					IStreamMonitor monitor = new StreamMonitor(this,
							remoteHost, socket);
					this.setConnected(remoteHost, monitor);

					// Let's start a thread for monitoring the input stream of
					// this socket
					Thread runner = new Thread(monitor);
					runner.start();
				} else {
					// Connection rejected
					socket.close();
				}
			} catch (Exception e) {
				Logger.getGlobal().log(Level.SEVERE,
						"Could not establish connection!", e);
				this.completeRequest(seqNum);
				return false;
			}
			this.completeRequest(seqNum);
			return true;
		}
	}

	public void requestAttachOK(IHost remoteHost, Socket socket, int seqNum)
			throws P2PException {
		IPacket sPacket = new Packet(IProtocol.PROTOCOL, IProtocol.ATTACH_OK,
				remoteHost.toString());
		sPacket.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
		sPacket.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
		sPacket.setHeader(IProtocol.SEQ_NUM, seqNum + "");

		try {
			sPacket.toStream(socket.getOutputStream());

			IStreamMonitor monitor = new StreamMonitor(this, remoteHost, socket);
			this.setConnected(remoteHost, monitor);

			// Let's start a thread for monitoring the input stream of this
			// socket
			Thread runner = new Thread(monitor);
			runner.start();
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE,
					"Could not send attach ok message to remote peer", e);
		}
	}

	public void requestAttachNOK(IHost remoteHost, Socket socket, int seqNum)
			throws P2PException {
		IPacket sPacket = new Packet(IProtocol.PROTOCOL, IProtocol.ATTACH_NOK,
				remoteHost.toString());
		sPacket.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
		sPacket.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
		sPacket.setHeader(IProtocol.SEQ_NUM, seqNum + "");

		try {
			sPacket.toStream(socket.getOutputStream());
			socket.close();
			Logger.getGlobal().log(Level.INFO,
					"Connection rejected to " + remoteHost);
		} catch (Exception e) {
			Logger.getGlobal().log(Level.SEVERE,
					"Could not send attach ok message to remote peer", e);
		}
	}

	@Override
	public void requestDetach(IHost remoteHost) throws P2PException {
		synchronized (this.hostToInStreamMonitor) {
			if (!this.hostToInStreamMonitor.containsKey(remoteHost))
				return;

			IPacket sPacket = new Packet(IProtocol.PROTOCOL, IProtocol.DETACH,
					remoteHost.toString());
			sPacket.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
			sPacket.setHeader(IProtocol.PORT, this.localhost.getPort() + "");

			IStreamMonitor monitor = this.hostToInStreamMonitor
					.remove(remoteHost);
			Socket socket = monitor.getSocket();
			sPacket.toStream(monitor.getOutputStream());

			try {
				socket.close();
			} catch (Exception e) {
				Logger.getGlobal().log(Level.WARNING, "Error closing socket!",
						e);
			}
			this.fireDisconnected(remoteHost);
		}
	}

	@Override
	public void requestList(IHost remoteHost) throws P2PException {
		IStreamMonitor monitor = this.hostToInStreamMonitor.get(remoteHost);

		if (monitor == null) {
			throw new P2PException("No connection exists to " + remoteHost);
		}

		int seqNum = this.newSequenceNumber();
		IPacket packet = new Packet(IProtocol.PROTOCOL, IProtocol.LIST,
				remoteHost.toString());
		packet.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
		packet.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
		packet.setHeader(IProtocol.SEQ_NUM, seqNum + "");

		this.logRequest(seqNum, packet);
		packet.toStream(monitor.getOutputStream());
	}

	@Override
	public void requestListing(IHost remoteHost, int seqNum)
			throws P2PException {
		IStreamMonitor monitor = this.hostToInStreamMonitor.get(remoteHost);

		if (monitor == null) {
			throw new P2PException("No connection exists to " + remoteHost);
		}

		StringBuilder builder = new StringBuilder();
		File dir = new File(this.getRootDirectory());
		for (File f : dir.listFiles()) {
			if (f.isFile()) {
				builder.append(f.getName());
				builder.append(IProtocol.CRLF);
			}
		}

		try {
			byte[] payload = builder.toString().getBytes(IProtocol.CHAR_SET);

			IPacket packet = new Packet(IProtocol.PROTOCOL, IProtocol.LISTING,
					remoteHost.toString());
			packet.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
			packet.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
			packet.setHeader(IProtocol.SEQ_NUM, seqNum + "");
			packet.setHeader(IProtocol.PAYLOAD_SIZE, payload.length + "");

			OutputStream out = monitor.getOutputStream();
			packet.toStream(out);
			out.write(payload);
		} catch (Exception e) {
			throw new P2PException(e);
		}
	}

	@Override
	public void requestGet(IHost remoteHost, String file) throws P2PException {
		IStreamMonitor monitor = this.hostToInStreamMonitor.get(remoteHost);

		if (monitor == null) {
			throw new P2PException("No connection exists to " + remoteHost);
		}

		int seqNum = this.newSequenceNumber();
		IPacket packet = new Packet(IProtocol.PROTOCOL, IProtocol.GET,
				remoteHost.toString());
		packet.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
		packet.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
		packet.setHeader(IProtocol.SEQ_NUM, seqNum + "");
		packet.setHeader(IProtocol.FILE_NAME, file);

		this.logRequest(seqNum, packet);
		packet.toStream(monitor.getOutputStream());
	}

	@Override
	public void requestPut(IHost remoteHost, String file, int seqNum)
			throws P2PException {
		IStreamMonitor monitor = this.hostToInStreamMonitor.get(remoteHost);

		if (monitor == null) {
			throw new P2PException("No connection exists to " + remoteHost);
		}

		File fileObj = new File(this.getRootDirectory()
				+ IProtocol.FILE_SEPERATOR + file);

		IPacket packet = null;
		if (fileObj.exists() && fileObj.isFile()) {
			packet = new Packet(IProtocol.PROTOCOL, IProtocol.PUT,
					remoteHost.toString());
			packet.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
			packet.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
			packet.setHeader(IProtocol.SEQ_NUM, seqNum + "");
			packet.setHeader(IProtocol.FILE_NAME, file);
			packet.setHeader(IProtocol.PAYLOAD_SIZE, fileObj.length() + "");
		} else {
			packet = new Packet(IProtocol.PROTOCOL, IProtocol.GET_NOK,
					remoteHost.toString());
			packet.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
			packet.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
			packet.setHeader(IProtocol.SEQ_NUM, seqNum + "");
			packet.setHeader(IProtocol.FILE_NAME, file);
		}

		packet.toStream(monitor.getOutputStream());
	}

	@Override
	public void discover(int depth) throws P2PException {
		// TODO: Complete the implementation
	}

	@Override
	public void find(String searchTerm, boolean exactMatch, int depth) throws P2PException {
		// TODO Search locally maybe?

		int seqNum = newSequenceNumber();
		forwardFileSearch(searchTerm, exactMatch, getLocalHost(), getLocalHost(),
				depth, seqNum);
	}

	@Override
	public void requestFileSearch(final String searchTerm,
			final boolean mustMatch, IHost respondTo, int sequence)
			throws P2PException {
		File rootDir = new File(getRootDirectory());

		File[] satisfyingFiles = rootDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (mustMatch ? name.equalsIgnoreCase(searchTerm) : name
						.toLowerCase().contains(searchTerm.toLowerCase()));
			}
		});

		if (satisfyingFiles == null || satisfyingFiles.length <= 0) {
			return; // no matches
		}

		StringBuilder builder = new StringBuilder();
		for (File file : satisfyingFiles) {
			builder.append(file.getName());
			builder.append(IProtocol.CRLF);
		}

		try {
			byte[] responsePayload = builder.toString().getBytes(
					IProtocol.CHAR_SET);

			IPacket found = new Packet(IProtocol.PROTOCOL, IProtocol.FOUND,
					respondTo.toString());
			found.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
			found.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
			found.setHeader(IProtocol.SEQ_NUM, sequence + "");
			found.setHeader(IProtocol.PAYLOAD_SIZE, responsePayload.length + "");

			IStreamMonitor monitor = hostToInStreamMonitor.get(respondTo);
			if (monitor == null) {
				// Need to connect then send the packet
				if (!requestAttach(respondTo)) {
					throw new P2PException(
							"Failed to connect to the Searcher for a FOUND response.");
				}
			}

			OutputStream out = monitor.getOutputStream();
			found.toStream(out);
			out.write(responsePayload);

		} catch (Exception e) {
			throw new P2PException(e);
		}
	}

	@Override
	public void forwardFileSearch(String searchTerm, boolean mustMatch,
			IHost searcher, IHost sender, int depth, int seqNum)
			throws P2PException {

		if (hostToInStreamMonitor.size() == 0) {
			// 1 host implies we can only send to the host that would've sent
			// the search
			requestSearched(sender, searcher, seqNum);
			return;
		}

		synchronized (activeSearches) {
			if (getActiveSearch(seqNum, searcher) != null) {
				return; // We're already doing this search
			}

			FileSearch search = new FileSearch();
			search.ForwardedCount = hostToInStreamMonitor.size();
			search.RepliedCount = 0;
			search.LastHopSender = sender;
			search.Searcher = searcher;

			saveActiveSearch(seqNum, searcher, search);
		}

		IPacket findRequest = new Packet(IProtocol.PROTOCOL, IProtocol.FIND,
				searcher.toString());

		findRequest.setHeader(IProtocol.HOST, this.localhost.getHostAddress());
		findRequest.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
		findRequest.setHeader(IProtocol.SEQ_NUM, seqNum + "");

		findRequest.setHeader(IProtocol.SEARCHER_HOST,
				searcher.getHostAddress());
		findRequest.setHeader(IProtocol.SEARCHER_PORT, searcher.getPort() + "");
		findRequest.setHeader(IProtocol.SEARCH_DEPTH, depth + "");

		if (mustMatch) {
			findRequest.setHeader(IProtocol.MATCHES, searchTerm);
		} else {
			findRequest.setHeader(IProtocol.CONTAINS, searchTerm);
		}

		List<IHost> excludedHosts = new ArrayList<IHost>();
		excludedHosts.add(searcher);
		excludedHosts.add(sender);

		if (broadcast(findRequest, excludedHosts) == 0) {
			requestSearched(sender, searcher, seqNum);
		}
	}

	@Override
	public void requestSearched(IHost sender, IHost searcher, int seqNum)
			throws P2PException {
		IPacket searchComplete = new Packet(IProtocol.PROTOCOL,
				IProtocol.SEARCHED, sender.toString());
		searchComplete.setHeader(IProtocol.HOST,
				this.localhost.getHostAddress());
		searchComplete.setHeader(IProtocol.PORT, this.localhost.getPort() + "");
		searchComplete.setHeader(IProtocol.SEARCHER_HOST,
				searcher.getHostAddress());
		searchComplete.setHeader(IProtocol.SEARCHER_PORT, searcher.getPort()
				+ "");
		searchComplete.setHeader(IProtocol.SEQ_NUM, seqNum + "");

		IStreamMonitor monitor = hostToInStreamMonitor.get(sender);

		FileSearch activeSearch = getActiveSearch(seqNum, searcher);

		if (activeSearch != null) {
			synchronized (activeSearch) {
				activeSearch.RepliedCount++;
			}
		}

		boolean searchFinished = activeSearch == null
				|| activeSearch.RepliedCount == activeSearch.ForwardedCount;
		boolean amSearcher = activeSearch == null
				|| activeSearch.Searcher == getLocalHost();

		if (amSearcher) {
			if (activeSearch != null && activeSearch.ForwardedCount > 0) {
				fireFoundProgress((int) Math
						.round((activeSearch.RepliedCount / activeSearch.ForwardedCount) * 100));
			} else {
				fireFoundProgress(PROGRESS_COMPLETE);
			}
		}

		if (searchFinished) {
			completeSearch(seqNum, searcher);
		}

		if (searchFinished && monitor != null && !amSearcher) {
			searchComplete.toStream(monitor.getOutputStream());
		}
	}

	private int broadcast(IPacket packet, List<IHost> exclude) {
		int peerCount = 0;
		for (IHost peer : hostToInStreamMonitor.keySet()) {
			if (exclude.contains(peer)) {
				continue;
			}

			IStreamMonitor monitor = hostToInStreamMonitor.get(peer);
			OutputStream out = monitor.getOutputStream();
			try {
				packet.toStream(out);
				peerCount++;
			} catch (Exception e) {
				System.out
						.println("Error broadcasting packet to peers; will proceed.");
				System.out.println(e);
				continue;
			}
		}
		return peerCount;
	}

	@Override
	public IPacket getRequest(int number) {
		return this.requestLog.get(number);
	}

	@Override
	public void logRequest(int number, IPacket p) {
		this.requestLog.put(number, p);
		this.fireRequestLogChanged();
	}

	@Override
	public void completeRequest(int number) {
		IPacket p = this.requestLog.remove(number);

		if (p != null)
			this.fireRequestLogChanged();
	}

	private String activeSearchKey(int number, IHost host) {
		return String.format("%d:%s:%d", number, host.getHostAddress(),
				host.getPort());
	}

	@Override
	public FileSearch getActiveSearch(int number, IHost host) {
		return activeSearches.get(activeSearchKey(number, host));
	}

	@Override
	public void saveActiveSearch(int number, IHost host, FileSearch search) {
		activeSearches.put(activeSearchKey(number, host), search);
	}

	@Override
	public void completeSearch(int number, IHost host) {
		activeSearches.remove(activeSearchKey(number, host));
	}

	/************************** Listeners Related Code *********************************/

	@Override
	public void addDownloadListener(IDownloadListener l) {
		this.downloadListeners.add(l);
	}

	@Override
	public void addListingListener(IListingListener l) {
		this.listingListeners.add(l);
	}

	@Override
	public void addRequestLogListener(IRequestLogListener l) {
		this.requestLogListeners.add(l);
	}

	@Override
	public void addConnectionListener(IConnectionListener l) {
		this.connectionListeners.add(l);
	}

	@Override
	public void addActivityListener(IActivityListener l) {
		this.activityListeners.add(l);
	}

	@Override
	public void fireConnected(IHost host) {
		synchronized (this.connectionListeners) {
			for (IConnectionListener c : this.connectionListeners) {
				c.connectionEstablished(host);
			}
		}
	}

	@Override
	public void fireDisconnected(IHost host) {
		synchronized (this.connectionListeners) {
			for (IConnectionListener c : this.connectionListeners) {
				c.connectionTerminated(host);
			}
		}
	}

	@Override
	public void fireDownloadComplete(IHost host, String file) {
		synchronized (this.downloadListeners) {
			for (IDownloadListener d : this.downloadListeners) {
				d.downloadComplete(host, file);
			}
		}
	}

	@Override
	public void fireListingReceived(IHost host, List<String> listing) {
		synchronized (this.listingListeners) {
			for (IListingListener l : this.listingListeners) {
				l.listingReceived(host, listing);
			}
		}
	}

	@Override
	public void fireRequestLogChanged() {
		synchronized (this.requestLogListeners) {
			for (IRequestLogListener l : this.requestLogListeners) {
				l.requestLogChanged(Collections
						.unmodifiableCollection(this.requestLog.values()));
			}
		}
	}

	@Override
	public void fireActivityPerformed(String message, IPacket p) {
		synchronized (this.activityListeners) {
			for (IActivityListener l : this.activityListeners) {
				l.activityPerformed(message, p);
			}
		}
	}

	@Override
	public void addFindListener(IFindListener l) {
		this.findListeners.add(l);
	}

	@Override
	public void fireFoundFile(IHost host, List<String> fileNames) {
		synchronized (findListeners) {
			for (IFindListener listener : findListeners) {
				listener.filesFound(host, fileNames);
			}
		}
	}

	@Override
	public void addFindProgressListener(IFindProgressListener l) {
		this.findProgressListeners.add(l);
	}

	@Override
	public void fireFoundProgress(int progress) {
		synchronized (findProgressListeners) {
			for (IFindProgressListener listener : findProgressListeners) {
				listener.findProgressChanged(progress);
			}
		}
	}
}
