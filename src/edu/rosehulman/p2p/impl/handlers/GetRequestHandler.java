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

public class GetRequestHandler extends AbstractHandler implements IRequestHandler {

	public GetRequestHandler(IP2PMediator mediator) {
		super(mediator);
	}

	@Override
	public void handle(IPacket packet, InputStream in) throws P2PException {
		try {
			int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));
			
			String host = packet.getHeader(IProtocol.HOST);
			int port = Integer.parseInt(packet.getHeader(IProtocol.PORT));
			IHost remoteHost = new Host(host, port);
			
			String fileName = packet.getHeader(IProtocol.FILE_NAME);
			
			mediator.requestPut(remoteHost, fileName, seqNum);
		}
		catch(Exception e) {
			throw new P2PException(e);
		}
	}
}
