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

import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.rosehulman.p2p.protocol.IHost;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;

/**
 * @author Chandan R. Rupakheti(chandan.rupakheti@rose-hulman.edu)
 *
 */
public class ConnectionMonitor implements Runnable {
	IP2PMediator mediator;
	volatile boolean stop;

	public ConnectionMonitor(IP2PMediator mediator) {
		this.mediator = mediator;
		this.stop = false;
	}
	
	public void stop() {
		this.stop = true;
		try {
			IHost localhost = mediator.getLocalHost();
			Socket socket = new Socket(localhost.getHostAddress(), localhost.getPort());
			socket.close();
		}
		catch(Exception e){}
	}

	@Override
	public void run() {
		IHost localHost = mediator.getLocalHost();
		try {
			ServerSocket server = new ServerSocket(localHost.getPort());
			while(true) {
				Socket socket = server.accept();
				
				if(stop)
					break;
				
				SetupThread setupRunner = new SetupThread(socket);
				setupRunner.start();
			}
			server.close();
		}
		catch(Exception e) {
			Logger.getGlobal().log(Level.SEVERE, "Could not start the peer server at " + localHost, e);
		}
	}
	
	private class SetupThread extends Thread {
		Socket socket;

		public SetupThread(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				IPacket packet = new Packet();
				packet.fromStream(socket.getInputStream());
				
				String host = packet.getHeader(IProtocol.HOST);
				int port = Integer.parseInt(packet.getHeader(IProtocol.PORT));
				int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));

				IHost remoteHost = new Host(host, port);
				String command = packet.getCommand();
				
				if(command.equalsIgnoreCase(IProtocol.ATTACH)) {
					mediator.requestAttachOK(remoteHost, socket, seqNum);
				}
				else {
					mediator.requestAttachNOK(remoteHost, socket, seqNum);
				}
			}
			catch(Exception e) {
				Logger.getGlobal().log(Level.WARNING, "Error intiating connection to a remote peer", e);
			}
		}
	}
}
