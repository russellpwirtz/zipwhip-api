package com.zipwhip.api.dto;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 7/6/11
 * Time: 2:03 PM
 * <p/>
 * Represents the transmission progress of a message.
 */
public class MessageProgress {

    int code;
    String key;
    String desc;
    String stateName;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> MessageProgress details:");
        toStringBuilder.append("\nCode: ").append(code);
        toStringBuilder.append("\nKey: ").append(key);
        toStringBuilder.append("\nDescription: ").append(desc);
        toStringBuilder.append("\nState: ").append(stateName);

        return toStringBuilder.toString();
    }

}
