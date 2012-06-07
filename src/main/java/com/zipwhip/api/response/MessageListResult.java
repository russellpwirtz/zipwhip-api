package com.zipwhip.api.response;

import com.zipwhip.api.dto.Message;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 6/6/12
 * Time: 11:56 AM
 */
public class MessageListResult {

    private List<Message> messages;
    private int start;
    private int limit;
    private int total;

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
