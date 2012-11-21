package com.zipwhip.api;

import com.zipwhip.concurrent.ConfiguredFactory;
import com.zipwhip.lifecycle.DestroyableBase;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
* Created with IntelliJ IDEA.
* User: Michael
* Date: 9/11/12
* Time: 5:22 PM
* To change this template use File | Settings | File Templates.
*/
public class NingApiConnectionFactory extends ApiConnectionFactory {

    private ConfiguredFactory<String, ExecutorService> workerExecutorFactory;

    @Override
    protected ApiConnection createInstance() {
        Executor workerExecutor = null;
        if (workerExecutorFactory != null) {
            workerExecutor = workerExecutorFactory.create("ApiConnection-worker");
        }

        NingHttpConnection connection = new NingHttpConnection(workerExecutor);

        final Executor finalWorkerExecutor = workerExecutor;
        if (finalWorkerExecutor != null) {
            connection.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService) finalWorkerExecutor).shutdownNow();
                }
            });
        }

        return connection;
    }

    public NingApiConnectionFactory workerExecutorFactory(ConfiguredFactory<String, ExecutorService> workerExecutorFactory) {
        this.workerExecutorFactory = workerExecutorFactory;
        return this;
    }
}
