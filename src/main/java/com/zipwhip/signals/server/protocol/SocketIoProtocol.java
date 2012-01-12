/**
 * 
 */
package com.zipwhip.signals.server.protocol;

import java.util.regex.Pattern;

/**
 * @author jdinsel
 *
 */
public class SocketIoProtocol {

	private static final String HEARTBEAT_INFO = ":1680:1800:"; // 28minutes:30minutes
	private static final Pattern socketIoPattern = Pattern.compile(":");
	private static final int LIMIT = 4;
	public static final long NO_ID = -1;

	public static final char DISCONNECT = '0';
	public static final char CONNECT = '1';
	public static final char HEART_BEAT = '2';
	public static final char MESSAGE = '3';
	public static final char JSON_MESSAGE = '4';
	public static final char EVENT = '5';
	public static final char ACK = '6';
	public static final char ERROR = '7';
	public static final char NOOP = '8';

	private static final int CLIENT_ID_LENGTH = 5;

	public static final String NOOP_MESSAGE = "8::";
	private static final String supportedProtocols = "websocket,xhr-polling,rawsocket";

	/**
	 * Format a Socket.IO style message as a JSON Message or a standard Message depending upon the content.
	 * 
	 * @param message
	 * @return
	 */
	public static String encode(long messageId, String message) {
		if (message.charAt(0) == '{') {
			message = jsonMessageResponse(messageId, message);
		} else {
			message = messageResponse(messageId, message);
		}

		return message;
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isDisconnectCommand(String message) {
		return SocketIoProtocol.DISCONNECT == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isConnectCommand(String message) {
		return SocketIoProtocol.CONNECT == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isHeartBeatCommand(String message) {
		return SocketIoProtocol.HEART_BEAT == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isMessageCommand(String message) {
		return SocketIoProtocol.MESSAGE == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isJsonMessageCommand(String message) {
		return SocketIoProtocol.JSON_MESSAGE == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isEventCommand(String message) {
		return SocketIoProtocol.EVENT == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isAckCommand(String message) {
		return SocketIoProtocol.ACK == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isErrorCommand(String message) {
		return SocketIoProtocol.ERROR == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static boolean isNoopCommand(String message) {
		return SocketIoProtocol.NOOP == message.charAt(0);
	}

	/**
	 * @param message
	 * @return
	 */
	public static long getClientId(String message) {
		String[] blocks = socketIoPattern.split(message, LIMIT);

		if (blocks.length >= 3) {
			String possibleId = blocks[2];
			if ((possibleId != null) && (possibleId.length() >= CLIENT_ID_LENGTH)) {
				return Long.parseLong(possibleId);
			}
		}
		return NO_ID;

	}

	/**
	 * The Socket.IO specification requires that upon connection, a string is returned with the
	 * id:heartBeat:disconnect:supportedTransports such as 1234567890:30:10:websockets
	 * 
	 * @param clientId
	 * @return
	 */
	public static String connectResponse(Long clientId) {
		return clientId + HEARTBEAT_INFO + supportedProtocols;
	}

	public static String messageResponse(long messageId, String message) {
		return baseMessageResponse(MESSAGE, messageId, message, null);
	}

	public static String jsonMessageResponse(long messageId, String message) {
		return baseMessageResponse(JSON_MESSAGE, messageId, message, null);
	}

	public static String connectMessageResponse(String message) {
		return connectMessageResponse(message, null);
	}

	public static String connectMessageResponse(String message, String clientId) {
		return baseMessageResponse(CONNECT, 0l, message, clientId);
	}

	public static String heartBeatMessageResponse(long messageId, String message) {
		return baseMessageResponse(HEART_BEAT, messageId, message, null);
	}

	private static final String baseMessageResponse(char messageType, long messageId, String message, String clientId) {
		return messageType + ":" + messageId + ":" + (clientId == null ? "" : clientId) + ":" + message + "\n";
	}

	/**
	 * Object a message or a json message
	 * 
	 * @param message
	 * @return
	 */
	public static String extractCommand(String message) {

		String[] blocks = socketIoPattern.split(message, LIMIT);
		if (blocks.length >= 3) {
			return blocks[3];
		}
		return null;
	}
}
