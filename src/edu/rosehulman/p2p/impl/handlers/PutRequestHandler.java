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

package edu.rosehulman.p2p.impl.handlers;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.rosehulman.p2p.impl.Host;
import edu.rosehulman.p2p.protocol.AbstractHandler;
import edu.rosehulman.p2p.protocol.IHost;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;
import edu.rosehulman.p2p.protocol.IRequestHandler;
import edu.rosehulman.p2p.protocol.P2PException;

/**
 * @author Chandan R. Rupakheti(chandan.rupakheti@rose-hulman.edu)
 *
 */
public class PutRequestHandler extends AbstractHandler implements IRequestHandler {

	public PutRequestHandler(IP2PMediator mediator) {
		super(mediator);
	}

	@Override
	public void handle(IPacket packet, InputStream in) throws P2PException {
		try {
			int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));

			String host = packet.getHeader(IProtocol.HOST);
			int port = Integer.parseInt(packet.getHeader(IProtocol.PORT));
			IHost remoteHost = new Host(host, port);
			
			IPacket rPacket = mediator.getRequest(seqNum);

			String fileName = packet.getHeader(IProtocol.FILE_NAME);
			int size = Integer.parseInt(packet.getHeader(IProtocol.PAYLOAD_SIZE));
			
			if(rPacket == null) {
				Logger.getGlobal().log(Level.INFO, "Ignoring put response for " + fileName + 
						"! The corresponding get request does not exists." );
				return;
			}
			
			FileOutputStream fOut = new FileOutputStream(mediator.getRootDirectory() + IProtocol.FILE_SEPERATOR + fileName);
			byte buffer[] = new byte[IProtocol.CHUNK_SIZE];
			int read = 0;
			while(read < size) {
				int currentRead = in.read(buffer);
				fOut.write(buffer, 0, currentRead);
				read += currentRead;
			}
			fOut.close();
			
			mediator.fireDownloadComplete(remoteHost, fileName);
		}
		catch(Exception e) {
			throw new P2PException(e);
		}
	}
}
