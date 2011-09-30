package com.zipwhip.api.signals;

import com.zipwhip.api.signals.commands.Command;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/19/11 Time: 4:37 PM
 * 
 * This is the basic envelope of a SignalServer packet.
 */
public class SignalMessage {

    // That's the body
    private Command command;
    private SignalAddress address;
    private SignalHeaders headers;

    public SignalMessage() {

    }

    public SignalMessage(Command command, SignalAddress address, SignalHeaders headers) {
        this.command = command;
        this.address = address;
        this.headers = headers;
    }

    public SignalMessage(Command command) {
        this.command = command;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public SignalAddress getAddress() {
        return address;
    }

    public void setAddress(SignalAddress address) {
        this.address = address;
    }

    public SignalHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(SignalHeaders headers) {
        this.headers = headers;
    }

}
