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

public class FindRequestHandler extends AbstractHandler implements
		IRequestHandler {

	public FindRequestHandler(IP2PMediator mediator) {
		super(mediator);
	}

	@Override
	public void handle(IPacket packet, InputStream in) throws P2PException {
		// Incoming FIND request
		try {
			int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));
			String host = packet.getHeader(IProtocol.HOST);
			int port = Integer.parseInt(packet.getHeader(IProtocol.PORT));
			int depth = Integer.parseInt(packet
					.getHeader(IProtocol.SEARCH_DEPTH));

			String searcherHost = packet.getHeader(IProtocol.SEARCHER_HOST);
			int searcherPort = Integer.parseInt(packet
					.getHeader(IProtocol.SEARCHER_PORT));

			IHost searcher = new Host(searcherHost, searcherPort);
			IHost sender = new Host(host, port);

			String match = packet.getHeader(IProtocol.MATCHES);
			String contains = packet.getHeader(IProtocol.CONTAINS);

			String searchTerm = null;
			boolean matches = true;
			if (match == null || match.isEmpty()) {
				if (contains == null || contains.isEmpty()) {
					throw new P2PException(
							"FIND Request received without a MATCHES nor CONTAINS header");
				} else {
					searchTerm = contains;
					matches = false;
				}
			} else {
				searchTerm = match;
			}

			mediator.requestFileSearch(searchTerm, matches, searcher, seqNum);

			if (depth > 0) {
				depth--;
				mediator.logRequest(seqNum, packet);
				mediator.forwardFileSearch(searchTerm, matches, searcher,
						sender, depth, seqNum);
			} else {
				mediator.requestSearched(sender, searcher, seqNum);
			}

		} catch (Exception e) {
			System.out.println("Should see what happened.");
			System.out.println(e);
			e.printStackTrace();

		}
	}

}
