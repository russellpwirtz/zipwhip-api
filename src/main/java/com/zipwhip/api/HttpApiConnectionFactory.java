package com.zipwhip.api;

import com.zipwhip.api.connection.HttpConnection;
import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.lifecycle.DestroyableBase;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/11/12
* Time: 5:23 PM
* To change this template use File | Settings | File Templates.
*/
public class HttpApiConnectionFactory extends ApiConnectionFactory {

    private CommonExecutorFactory executorFactory;

    @Override
    protected ApiConnection createInstance() {
        return new HttpConnection(executorFactory);
    }

    public CommonExecutorFactory getExecutorFactory() {
        return executorFactory;
    }

    public void setExecutorFactory(CommonExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }
}
