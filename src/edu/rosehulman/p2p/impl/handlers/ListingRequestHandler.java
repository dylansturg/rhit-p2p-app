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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
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
public class ListingRequestHandler extends AbstractHandler implements IRequestHandler {

	public ListingRequestHandler(IP2PMediator mediator) {
		super(mediator);
	}

	@Override
	public void handle(IPacket packet, InputStream in) throws P2PException {
		String host = packet.getHeader(IProtocol.HOST);
		int port = Integer.parseInt(packet.getHeader(IProtocol.PORT));
		IHost remoteHost = new Host(host, port);
		int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));
		int payloadSize = Integer.parseInt(packet.getHeader(IProtocol.PAYLOAD_SIZE));
		
		IPacket rPacket = mediator.getRequest(seqNum);
		if(rPacket == null) {
			Logger.getGlobal().log(Level.INFO, "Ignoring listing response! The corresponding list request does not exists." );
			return;	
		}
		
		try {
			List<String> listing = new ArrayList<>();

			byte[] buffer = new byte[payloadSize];
			in.read(buffer);
			
			String listingStr = new String(buffer, IProtocol.CHAR_SET);
			StringTokenizer tokenizer = new StringTokenizer(listingStr);
			while(tokenizer.hasMoreTokens()) {
				String file = tokenizer.nextToken(IProtocol.LF).trim();
				if(!file.isEmpty()) {
					listing.add(file);
				}
			}
			
			// WE HAVE A FIRE??
			mediator.fireListingReceived(remoteHost, listing);
		}
		catch(Exception e) {
			throw new P2PException(e);
		}
	}
}
