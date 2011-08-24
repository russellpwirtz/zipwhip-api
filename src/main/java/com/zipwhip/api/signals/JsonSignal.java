package com.zipwhip.api.signals;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 7/5/11 Time: 8:11 PM
 * <p/>
 * Represents a Signal that was parsed from Json
 */
public class JsonSignal extends Signal {

    private static final long serialVersionUID = 757720958701072081L;
    private String json;

    public JsonSignal(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

}
