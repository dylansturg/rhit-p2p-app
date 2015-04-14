package edu.rosehulman.p2p.impl.handlers;

import java.io.OutputStream;

import edu.rosehulman.p2p.protocol.AbstractHandler;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;
import edu.rosehulman.p2p.protocol.IResponseHandler;
import edu.rosehulman.p2p.protocol.P2PException;

public class SearchedResponseHandler extends AbstractHandler implements
		IResponseHandler {

	public SearchedResponseHandler(IP2PMediator mediator) {
		super(mediator);
	}

	@Override
	public void handle(IPacket packet, OutputStream out) throws P2PException {
		int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));
		mediator.completeRequest(seqNum);
	}

}
