package com.zipwhip.important.tasks;

import com.zipwhip.important.ImportantTask;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:48 PM
 */
public class SimpleImportantTask<T extends Serializable> implements ImportantTask<T> {

    private String requestType;
    private String requestId = UUID.randomUUID().toString();
    private Date expirationDate = new Date(System.currentTimeMillis() + 30000); // now + 30 seconds
    private T parameters;

    public SimpleImportantTask(String requestType, T parameters, Date expirationDate, String requestId) {
        this.requestType = requestType;
        this.requestId = requestId;
        this.expirationDate = expirationDate;
        this.parameters = parameters;
    }

    public SimpleImportantTask(String requestType, T parameters, Date expirationDate) {
        this.requestType = requestType;
        this.parameters = parameters;
        this.expirationDate = expirationDate;
    }

    public SimpleImportantTask(String requestType, T parameters) {
        this.requestType = requestType;
        this.parameters = parameters;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getRequestId() {
        return requestId;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    @Override
    public T getParameters() {
        return parameters;
    }

}
