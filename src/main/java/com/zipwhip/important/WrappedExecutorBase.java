package com.zipwhip.important;

import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 3:29 PM
 */
public class WrappedExecutorBase {

    private ImportantTaskExecutor executor;
    private long timeout = 30;
    private TimeUnit units = TimeUnit.SECONDS;

    public WrappedExecutorBase(ImportantTaskExecutor executor, long timeout, TimeUnit units) {
        this.executor = executor;
        this.timeout = timeout;
        this.units = units;
    }

    public WrappedExecutorBase(ImportantTaskExecutor executor) {
        this.executor = executor;
    }

    public ImportantTaskExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ImportantTaskExecutor executor) {
        this.executor = executor;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public TimeUnit getUnits() {
        return units;
    }

    public void setUnits(TimeUnit units) {
        this.units = units;
    }
}
