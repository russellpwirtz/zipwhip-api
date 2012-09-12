package com.zipwhip.api;

import com.zipwhip.concurrent.ConfiguredFactory;
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

    private ConfiguredFactory<String, ExecutorService> bossExecutorFactory;
    private ConfiguredFactory<String, ExecutorService> workerExecutorFactory;

    @Override
    protected ApiConnection createInstance() {
        Executor bossExecutor = null;
        if (bossExecutorFactory != null) {
            bossExecutor = bossExecutorFactory.create("ApiConnection-boss");
        }
        Executor workerExecutor = null;
        if (workerExecutorFactory != null) {
            workerExecutor = workerExecutorFactory.create("ApiConnection-worker");
        }

        HttpConnection connection = new HttpConnection(bossExecutor, workerExecutor);

        final Executor finalBossExecutor = bossExecutor;
        if (finalBossExecutor != null) {
            connection.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ((ExecutorService) finalBossExecutor).shutdownNow();
                }
            });
        }

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

    public ConfiguredFactory<String, ExecutorService> getBossExecutorFactory() {
        return bossExecutorFactory;
    }

    public void setBossExecutorFactory(ConfiguredFactory<String, ExecutorService> bossExecutorFactory) {
        this.bossExecutorFactory = bossExecutorFactory;
    }

    public ConfiguredFactory<String, ExecutorService> getWorkerExecutorFactory() {
        return workerExecutorFactory;
    }

    public void setWorkerExecutorFactory(ConfiguredFactory<String, ExecutorService> workerExecutorFactory) {
        this.workerExecutorFactory = workerExecutorFactory;
    }
}
