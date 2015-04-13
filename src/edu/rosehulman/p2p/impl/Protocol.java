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

import java.util.HashMap;
import java.util.Map;

import edu.rosehulman.p2p.protocol.IProtocol;
import edu.rosehulman.p2p.protocol.IRequestHandler;
import edu.rosehulman.p2p.protocol.IResponseHandler;

/**
 * @author Chandan R. Rupakheti (chandan.rupakheti@rose-hulman.edu)
 *
 */
public class Protocol implements IProtocol {
	private static final Protocol instance = new Protocol();
	
	public static Protocol getInstance() {
		return instance;
	}
	
	protected Map<String, IRequestHandler> reqeustHandlers;
	protected Map<String, IResponseHandler> responseHandlers;

	public Protocol() {
		this.reqeustHandlers = new HashMap<>();
		this.responseHandlers = new HashMap<>();
	}
	
	@Override
	public void setRequestHandler(String command, IRequestHandler handler) {
		this.reqeustHandlers.put(command, handler);
	}

	@Override
	public IRequestHandler getRequestHandler(String command) {
		return this.reqeustHandlers.get(command);
	}

	@Override
	public void setResponseHandler(String command, IResponseHandler handler) {
		this.responseHandlers.put(command, handler);
	}

	@Override
	public IResponseHandler getResponseHandler(String command) {
		return this.responseHandlers.get(command);
	}
}
