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

import java.io.File;

public interface IProtocol {
	public static final String CR = "\r";
	public static final String LF = "\n";
	public static final String CRLF = "\r\n";
	public static final String CHAR_SET = "UTF-8";
	public static final String SEPERATOR = ":";
	public static final String SPACE = " ";

	public static final String PROTOCOL = "P2P1.0";

	public static final String SEQ_NUM = "Sequence";

	public static final String ATTACH = "ATTACH";
	public static final String ATTACH_OK = "ATTACH_OK";
	public static final String ATTACH_NOK = "ATTACH_NOK";
	public static final String DETACH = "DETACH";

	public static final String DISCOVER = "DISCOVER";

	public static final String LIST = "LIST";
	public static final String LISTING = "LISTING";

	public static final String GET = "GET";
	public static final String GET_NOK = "GET_NOK";
	public static final String PUT = "PUT";

	public static final String FIND = "FIND";
	public static final String FOUND = "FOUND";
	public static final String SEARCHED = "SEARCHED";

	public static final String HOST = "Host";
	public static final String PORT = "Port";

	public static final String FILE_NAME = "File-Name";
	public static final String PAYLOAD_SIZE = "Payload-Size";

	public static final String SEARCHER_HOST = "Searcher-Host";
	public static final String SEARCHER_PORT = "Searcher-Port";
	public static final String SEARCH_DEPTH = "Search-Depth";
	public static final String CONTAINS = "Contains";
	public static final String MATCHES = "Matches";

	public static final int CHUNK_SIZE = 1024;
	public static final String FILE_SEPERATOR = File.separator;// System.getProperty("file.seperator");

	public static final String MESSAGE = "Message";

	public void setRequestHandler(String command, IRequestHandler handler);

	public IRequestHandler getRequestHandler(String command);

	public void setResponseHandler(String command, IResponseHandler handler);

	public IResponseHandler getResponseHandler(String command);
}
