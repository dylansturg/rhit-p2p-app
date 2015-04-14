package edu.rosehulman.p2p.impl.handlers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.rosehulman.p2p.impl.Host;
import edu.rosehulman.p2p.protocol.AbstractHandler;
import edu.rosehulman.p2p.protocol.IP2PMediator;
import edu.rosehulman.p2p.protocol.IPacket;
import edu.rosehulman.p2p.protocol.IProtocol;
import edu.rosehulman.p2p.protocol.IRequestHandler;
import edu.rosehulman.p2p.protocol.P2PException;

public class FoundRequestHandler extends AbstractHandler implements
		IRequestHandler {

	public FoundRequestHandler(IP2PMediator mediator) {
		super(mediator);
	}

	@Override
	public void handle(IPacket packet, InputStream in) throws P2PException {
		int seqNum = Integer.parseInt(packet.getHeader(IProtocol.SEQ_NUM));
		String host = packet.getHeader(IProtocol.HOST);
		int port = Integer.parseInt(packet.getHeader(IProtocol.PORT));
		int payloadSize = Integer.parseInt(packet
				.getHeader(IProtocol.PAYLOAD_SIZE));

		IPacket rPack = mediator.getRequest(seqNum);
		if (rPack == null) {
			Logger.getGlobal().log(
					Level.INFO,
					"FOUND request ignoring response - supplied sequence ("
							+ seqNum + ") does not exist.");
		}

		try {
			List<String> fileNames = new ArrayList<String>();

			byte[] buffer = new byte[payloadSize];
			in.read(buffer);

			String listingStr = new String(buffer, IProtocol.CHAR_SET);
			StringTokenizer tokenizer = new StringTokenizer(listingStr);
			while (tokenizer.hasMoreTokens()) {
				String file = tokenizer.nextToken(IProtocol.LF).trim();
				if (!file.isEmpty()) {
					fileNames.add(file);
				}
			}

			mediator.fireFoundFile(new Host(host, port), fileNames);

		} catch (Exception e) {
			throw new P2PException(e);
		}

	}
}
