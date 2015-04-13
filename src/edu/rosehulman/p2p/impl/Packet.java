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
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;
import edu.rosehulman.p2p.protocol.IRequestHandler;
import edu.rosehulman.p2p.protocol.IResponseHandler;
import edu.rosehulman.p2p.protocol.P2PException;

public class Packet implements IPacket {
	String protocol;
	String command;
	String object;
	
	int size;
	Map<String, String> headers;
	
	public Packet() {
		this.headers = new HashMap<>();
	}

	public Packet(String protocol, String command, String object) {
		this.protocol = protocol;
		this.command = command;
		this.object = object;
		this.headers = new HashMap<>();
	}
	
	@Override
	public String getProtocol() {
		return this.protocol;
	}

	@Override
	public String getCommand() {
		return this.command;
	}

	@Override
	public String getObject() {
		return this.object;
	}

	@Override
	public String getHeader(String key) {
		return this.headers.get(key);
	}

	@Override
	public void setHeader(String key, String value) {
		this.headers.put(key, value);
	}

	@Override
	public void fromStream(InputStream in) throws P2PException {
		try {
			// Making sure nobody writes
			synchronized(in) {
				this.parseStatus(in);
				this.parseHeaders(in);
				
				Protocol protocol = Protocol.getInstance();
				IRequestHandler handler = protocol.getRequestHandler(this.command);
				
				if(handler != null)
					handler.handle(this, in);
			}
		}
		catch(Exception e) {
			throw new P2PException(e);
		}
	}

	@Override
	public void toStream(OutputStream out) throws P2PException {
		StringBuilder builder = new StringBuilder();
		
		this.buildStatus(builder);
		this.buildHeader(builder);
		
		// Let's send the status line and header before sending actual payload
		synchronized(out) {
			try {
				byte[] buffer = builder.toString().getBytes(IProtocol.CHAR_SET);
				out.write(buffer);
			}
			catch(Exception e) {
				throw new P2PException(e);
			}

			// Let's delegate the payload sending logic to the response handler
			Protocol protocol = Protocol.getInstance();
			IResponseHandler handler = protocol.getResponseHandler(this.command);

			if(handler != null)
				handler.handle(this, out);
		}
	}

	
	/* -------------------------- Helper Methods -------------------------- */
	
	String readLine(InputStream in) throws IOException {
		int c = in.read();
		StringBuilder builder = new StringBuilder();
		while(c != '\n' && c != -1) {
			builder.append((char)c);
			c = in.read();
		}
		
		return builder.toString().trim();	
	}
	
	void parseStatus(InputStream in) throws Exception {
		String statusLine = this.readLine(in);
		StringTokenizer tokenizer = new StringTokenizer(statusLine);
		
		this.protocol = tokenizer.nextToken(" ").trim();
		this.command = tokenizer.nextToken(" ").trim();
		
		if(tokenizer.hasMoreTokens())
			this.object = tokenizer.nextToken();
	}
	
	private void parseHeaders(InputStream in) throws Exception {
		String line = this.readLine(in);
		while(line != null && !line.isEmpty()) {
			int split = line.indexOf(IProtocol.SEPERATOR);
			String key = line.substring(0, split).trim();
			String value = line.substring(split + 1, line.length()).trim();
			this.headers.put(key, value);
			line = this.readLine(in);
		}
	}
	
	private void buildStatus(StringBuilder builder) {
		builder.append(this.protocol);
		builder.append(IProtocol.SPACE);
		builder.append(this.command);
		builder.append(IProtocol.SPACE);

		if(this.object != null)
			builder.append(this.object);

		builder.append(IProtocol.CRLF);
	}
	
	private void buildHeader(StringBuilder builder) {
		for(Map.Entry<String, String> entry : this.headers.entrySet()) {
			builder.append(entry.getKey());
			builder.append(IProtocol.SPACE);
			builder.append(IProtocol.SEPERATOR);
			builder.append(IProtocol.SPACE);
			builder.append(entry.getValue());
			builder.append(IProtocol.CRLF);
		}
		builder.append(IProtocol.CRLF);
	}

	/* ------------------------ End Helper Methods ------------------------- */
	
}
