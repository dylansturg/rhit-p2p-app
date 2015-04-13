package edu.rosehulman.p2p.protocol;

public class FileSearch {
	public IHost Searcher;
	public IHost LastHopSender;
	public int ForwardedCount;
	public int RepliedCount = 0;
}
