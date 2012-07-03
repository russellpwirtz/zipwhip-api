/**
 *
 */
package com.zipwhip.api.signals.commands;

import java.util.List;
import java.util.Map;

import com.zipwhip.signals.message.Action;

/**
 * @author jdinsel
 */
public final class SubscriptionCommand extends Command<Map<String, String>> {

    private static final long serialVersionUID = 1L;
    private static final Action action = Action.SUBSCRIBE;

    private String id;
    private List<Map<String, String>> data;

    public SubscriptionCommand() {
    }

    public SubscriptionCommand(String id, List<Map<String, String>> data) {
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

    public final List<Map<String, String>> getData() {
        return data;
    }

    public final void setData(List<Map<String, String>> data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionCommand)) return false;

        SubscriptionCommand that = (SubscriptionCommand) o;

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
