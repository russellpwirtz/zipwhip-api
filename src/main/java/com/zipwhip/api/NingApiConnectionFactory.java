package com.zipwhip.api;

import com.ning.http.client.ProxyServer;
import com.zipwhip.concurrent.ConfiguredFactory;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;

import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: Michael
 * Date: 9/11/12
 * Time: 5:22 PM
 */
public class NingApiConnectionFactory extends ApiConnectionFactory {

    private ConfiguredFactory<String, ExecutorService> workerExecutorFactory;

    public NingApiConnectionFactory() throws NoRouteToHostException {
        super();
    }

    public NingApiConnectionFactory(final Proxy proxy) throws NoRouteToHostException {
        super(proxy);
    }

    @Override
    protected ApiConnection createInstance() {
        Executor workerExecutor = null;
        if (workerExecutorFactory != null) {
            workerExecutor = workerExecutorFactory.create("ApiConnection-worker");
        }

        // fallback to a default one
        if (workerExecutor == null) workerExecutor = Executors.newFixedThreadPool(10);

        // Create the connection
        final ApiConnection connection = new NingHttpConnection(workerExecutor, getProxyServer());

        // Make sure we cleanup the executor
        final Executor finalWorkerExecutor = workerExecutor;
        if (finalWorkerExecutor != null) {
            ((CascadingDestroyableBase) connection).link(new DestroyableBase() {
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

    private ProxyServer getProxyServer() {
        if (getProxy() == null || getProxy().type() == null || Proxy.Type.DIRECT.equals(getProxy().type())
                || getProxy().address() == null || !(getProxy().address() instanceof InetSocketAddress)) return null;

        final InetSocketAddress address = (InetSocketAddress) getProxy().address();
        return new ProxyServer(address.getHostName(), address.getPort());
    }
}
