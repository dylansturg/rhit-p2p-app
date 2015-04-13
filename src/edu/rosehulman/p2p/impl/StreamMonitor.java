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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.rosehulman.p2p.protocol.IHost;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IStreamMonitor;
import edu.rosehulman.p2p.protocol.P2PException;

public class StreamMonitor implements IStreamMonitor {
	private IP2PMediator mediator;
	private IHost remoteHost;
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	
	volatile boolean stop;
	
	public StreamMonitor(IP2PMediator mediator, IHost remoteHost, Socket socket) throws IOException {
		this.mediator = mediator;
		this.remoteHost = remoteHost;
		
		this.socket = socket;
		this.in = this.socket.getInputStream();
		this.out = this.socket.getOutputStream();
		
		this.stop = false;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public InputStream getInputStream() {
		return this.in;
	}
	
	public OutputStream getOutputStream() {
		return this.out;
	}

	@Override
	public void run() {
		while(!this.stop) {
			IPacket packet = new Packet();
			try {
				packet.fromStream(this.in);
				// Note that there are handlers configured in TransportProtocol that
				// kick-in within the packet.fromStream() method to handle the 
				// packet further
			} 
			catch (P2PException e) {
				Logger.getGlobal().log(Level.SEVERE, "Error Receiving Packet!", e);
				this.stop();
			}
		}
	}
	
	public void stop() {
		if(this.stop)
			return;
		
		this.stop = true;
		try {
			this.mediator.requestDetach(this.remoteHost);
		}
		catch(Exception e){
			Logger.getGlobal().log(Level.WARNING, "Error Detaching from " + this.remoteHost + "!" , e);
		}
	}
}
