package edu.rosehulman.p2p.impl.handlers;

import java.io.InputStream;

import edu.rosehulman.p2p.impl.Host;
import edu.rosehulman.p2p.protocol.AbstractHandler;
import edu.rosehulman.p2p.protocol.IHost;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;
import edu.rosehulman.p2p.protocol.IRequestHandler;
import edu.rosehulman.p2p.protocol.P2PException;

public class SearchedRequestHandler extends AbstractHandler implements
		IRequestHandler {

	public SearchedRequestHandler(IP2PMediator mediator) {
		super(mediator);
	}

	@Override
	public void handle(IPacket packet, InputStream in) throws P2PException {
		// TODO Auto-generated method stub
		int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));
		String host = packet.getHeader(IProtocol.HOST);
		int port = Integer.parseInt(packet.getHeader(IProtocol.PORT));

		String searcherHost = packet.getHeader(IProtocol.SEARCHER_HOST);
		int searcherPort = Integer.parseInt(packet
				.getHeader(IProtocol.SEARCHER_PORT));

		IHost searcher = new Host(searcherHost, searcherPort);
		IHost sender = new Host(host, port);

		mediator.requestSearched(sender, searcher, seqNum);
	}

}
