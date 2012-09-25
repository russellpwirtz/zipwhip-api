package com.zipwhip.api.signals.sockets.netty.pipeline;

import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.sockets.netty.pipeline.SocketIoCommandDecoder;
import com.zipwhip.signals.server.protocol.SocketIoProtocol;
import org.junit.Test;

import static org.junit.Assert.*;

public class SocketIoCommandDecoderTest {

	@Test
	public void testConnectCommand() throws Exception {

		SocketIoCommandDecoder decoder = new SocketIoCommandDecoder();

		String response = "123456789012345678:1600:1780:rawsocket,websocket,xhr-polling";
		ConnectCommand command = (ConnectCommand) decoder.decode(null, null, response);
		assertEquals("123456789012345678", command.getClientId());
	}

	@Test
	public void testJsonCommand() throws Exception {

		SocketIoCommandDecoder decoder = new SocketIoCommandDecoder();
		String response = "4::{1234567890123456789:1600:1780:rawsocket,websocket,xhr-polling}";
		// This can't decode a bad json string!
		ConnectCommand command = (ConnectCommand) decoder.decode(null, null, response);
		assertNull(command);

		response = "{\"versions\":{\"heartbeat\":1600,\"disconnect\":1780},\"action\":\"CONNECT\",\"clientId\":\"1234567890123456789\"}\"";
		response = SocketIoProtocol.encode(1, response);
		assertTrue(SocketIoProtocol.isJsonMessageCommand(response));
		command = (ConnectCommand) decoder.decode(null, null, response);
		assertEquals("1234567890123456789", command.getClientId());
	}

}