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

package edu.rosehulman.p2p.protocol;

import java.net.Socket;
import java.util.List;

import edu.rosehulman.p2p.impl.notification.IActivityListener;
import edu.rosehulman.p2p.impl.notification.IConnectionListener;
import edu.rosehulman.p2p.impl.notification.IDownloadListener;
import edu.rosehulman.p2p.impl.notification.IFindListener;
import edu.rosehulman.p2p.impl.notification.IFindProgressListener;
import edu.rosehulman.p2p.impl.notification.IListingListener;
import edu.rosehulman.p2p.impl.notification.IRequestLogListener;

public interface IP2PMediator {
	public IHost getLocalHost();
	public String getRootDirectory();
	
	public void setConnected(IHost host, IStreamMonitor monitor);
	

	public IPacket getRequest(int number);
	public void logRequest(int number, IPacket p);
	public void completeRequest(int number);
	
	public FileSearch getActiveSearch(int number, IHost host);
	public void saveActiveSearch(int number, IHost host, FileSearch search);
	public void completeSearch(int number, IHost host);
	
	public boolean requestAttach(IHost host) throws P2PException;
	public void requestAttachOK(IHost host, Socket socket, int seqNum) throws P2PException;
	public void requestAttachNOK(IHost host, Socket socket, int seqNum) throws P2PException;

	public void requestDetach(IHost host) throws P2PException;

	public void discover(int depth) throws P2PException;
	public void find(String searchTerm, boolean exactMatch, int depth) throws P2PException;
	
	public void requestFileSearch(String searchTerm, boolean mustMatch, IHost respondTo, int sequence) throws P2PException;
	public void forwardFileSearch(String searchTerm, boolean mustMatch, IHost searcher, IHost sender, int depth, int seqNum) throws P2PException;
	public void requestSearched(IHost sender, IHost searcher, int seqNum) throws P2PException;
	
	public void requestList(IHost host) throws P2PException;	
	public void requestListing(IHost remoteHost, int seqNum) throws P2PException;
	
	public void requestGet(IHost host, String file) throws P2PException;
	public void requestPut(IHost remoteHost, String file, int seqNum) throws P2PException;
	
	public void addDownloadListener(IDownloadListener l);
	public void addListingListener(IListingListener l);
	public void addRequestLogListener(IRequestLogListener l);
	public void addConnectionListener(IConnectionListener l);
	public void addActivityListener(IActivityListener l);
	public void addFindListener(IFindListener l);
	public void addFindProgressListener(IFindProgressListener l);
	
	public void fireFoundProgress(int progress);
	public void fireFoundFile(IHost host, List<String> fileNames);
	public void fireDownloadComplete(IHost host, String file);
	public void fireListingReceived(IHost host, List<String> listing);
	public void fireRequestLogChanged();
	public void fireConnected(IHost host);
	public void fireDisconnected(IHost host);
	public void fireActivityPerformed(String message, IPacket p);
}
