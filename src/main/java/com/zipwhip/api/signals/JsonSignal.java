package com.zipwhip.api.signals;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 7/5/11 Time: 8:11 PM
 * <p/>
 * Represents a Signal that was parsed from Json
 */
public class JsonSignal extends Signal implements Serializable {

    private static final long serialVersionUID = 757720958701072081L;
    private String json;

    public JsonSignal(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonSignal)) return false;

        JsonSignal that = (JsonSignal) o;

        if (json != null ? !json.equals(that.json) : that.json != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return json != null ? json.hashCode() : 0;
    }

}
