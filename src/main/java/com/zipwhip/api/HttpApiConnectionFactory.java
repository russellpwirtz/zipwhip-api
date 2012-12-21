package com.zipwhip.api;

import com.zipwhip.executors.CommonExecutorFactory;
import com.zipwhip.executors.CommonExecutorTypes;
import com.zipwhip.lifecycle.DestroyableBase;

import java.net.NoRouteToHostException;
import java.net.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * User: Michael
 * Date: 9/11/12
 * Time: 5:23 PM
 */
public class HttpApiConnectionFactory extends ApiConnectionFactory {

    private CommonExecutorFactory executorFactory;

    public HttpApiConnectionFactory() throws NoRouteToHostException {
        super();
    }

    public HttpApiConnectionFactory(Proxy proxy) throws NoRouteToHostException {
        super(proxy);
    }

    @Override
    protected ApiConnection createInstance() {
        Executor bossExecutor = null;
        if (executorFactory != null) {
            bossExecutor = executorFactory.create(CommonExecutorTypes.BOSS, "ApiConnection");
        }
        Executor workerExecutor = null;
        if (executorFactory != null) {
            workerExecutor = executorFactory.create(CommonExecutorTypes.WORKER, "ApiConnection");
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

    public CommonExecutorFactory getExecutorFactory() {
        return executorFactory;
    }

    public void setExecutorFactory(CommonExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }
}
