/**
 *
 */
package com.zipwhip.api.signals.commands;

import java.util.List;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.signals.message.Action;

/**
 * @author jdinsel
 */
public class MessageCommand extends Command<Signal> {

    private static final long serialVersionUID = 1L;
    private static final Action action = Action.MESSAGE;

    private String id;
    private List<Signal> data;

    public MessageCommand(String id, List<Signal> data) {
        this.id = id;
        this.data = data;
    }

    @Override
    public Action getAction() {
        return action;
    }

    public final String getId() {
        return id;
    }

    public final void setId(String id) {
        this.id = id;
    }

    public final List<Signal> getData() {
        return data;
    }

    public final void setData(List<Signal> data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageCommand)) return false;

        MessageCommand that = (MessageCommand) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
