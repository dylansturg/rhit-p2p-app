package edu.rosehulman.p2p.impl.notification;

import java.util.List;

import edu.rosehulman.p2p.protocol.IHost;

public interface IFindListener {
	public void filesFound(IHost host, List<String> fileNames);
}
