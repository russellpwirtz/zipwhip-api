package com.zipwhip.executors;

import com.zipwhip.concurrent.ExecutorFactory;

import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 11/14/12
 * Time: 4:14 PM
 */
public class DefaultCommonExecutorFactory implements CommonExecutorFactory {

    private static final DefaultCommonExecutorFactory INSTANCE = new DefaultCommonExecutorFactory();

    @Override
    public ExecutorService create(CommonExecutorTypes type, String name) {
        return ExecutorFactory.newInstance(name);
    }

    @Override
    public ExecutorService create() {
        return create(CommonExecutorTypes.BOSS, "default");
    }

    public static DefaultCommonExecutorFactory getInstance() {
        return INSTANCE;
    }
}
