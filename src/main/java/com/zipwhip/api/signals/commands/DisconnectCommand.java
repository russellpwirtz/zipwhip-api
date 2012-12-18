package com.zipwhip.api.signals.commands;

import com.zipwhip.signals.message.Action;

/**
 * @author jed
 */
public class DisconnectCommand extends Command {

    private static final long serialVersionUID = 1L;
    public static final Action ACTION = Action.DISCONNECT; // "disconnect";

    private String host;
    private int port;
    private int reconnectDelay;
    private boolean stop;
    private boolean ban;

    /**
     * Create a new DisconnectCommand
     *
     * @param host           Host to reconnect to. Empty string indicates no change.
     * @param port           Port to reconnect to. Empty string indicates no change.
     * @param reconnectDelay Seconds to wait before reconnecting.
     * @param stop           If true do not reconnect.
     * @param ban            If true the server will refuse reconnect attempts with the same clientId.
     */
    public DisconnectCommand(String host, int port, int reconnectDelay, boolean stop, boolean ban) {
        this.host = host;
        this.port = port;
        this.reconnectDelay = reconnectDelay;
        this.stop = stop;
        this.ban = ban;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getReconnectDelay() {
        return reconnectDelay;
    }

    public boolean isStop() {
        return stop;
    }

    public boolean isBan() {
        return ban;
    }

    public void setBan(boolean ban) {
        this.ban = ban;
    }

    @Override
    public Action getAction() {
        return ACTION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DisconnectCommand)) return false;

        DisconnectCommand that = (DisconnectCommand) o;

        if (ban != that.ban) return false;
        if (port != that.port) return false;
        if (reconnectDelay != that.reconnectDelay) return false;
        if (stop != that.stop) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + reconnectDelay;
        result = 31 * result + (stop ? 1 : 0);
        result = 31 * result + (ban ? 1 : 0);
        return result;
    }
}
