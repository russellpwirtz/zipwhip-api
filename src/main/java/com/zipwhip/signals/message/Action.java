package com.zipwhip.signals.message;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 11, 2010
 * Time: 5:15:46 PM
 * 
 * The different command types we support.
 */
public enum Action {

	CONNECT, DISCONNECT, SUBSCRIBE, SUBSCRIBE_RESPONSE, ERROR, UNSUBSCRIBE, MESSAGE, SIGNAL, SUBSCRIPTION_COMPLETE, UNSUBSCRIPTION_COMPLETE, LOGOUT, SIGNAL_VERIFICATION, BACKLOG, PRESENCE, PING, PONG, NOOP, BACKFILL;

}
