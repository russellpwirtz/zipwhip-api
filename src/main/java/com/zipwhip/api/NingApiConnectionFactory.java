package com.zipwhip.api;

import com.ning.http.client.ProxyServer;
import com.zipwhip.concurrent.ConfiguredFactory;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.net.ProxyUtil;
import com.zipwhip.util.CollectionUtil;

import java.net.*;
import java.util.List;
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
    private Proxy proxy;

    /**
     * NingApiConnectionFactory constructor
     */
    public NingApiConnectionFactory() {
        this(Proxy.NO_PROXY);
    }

    /**
     * NingApiConnectionFactory constructor.
     * Need to call the setupProxy() method before creating an api connection instance
     *
     * @param proxy - connection proxy
     * @throws NullPointerException if proxy is null
     */
    public NingApiConnectionFactory(final Proxy proxy) {
        super();
        if (proxy == null) throw new NullPointerException("Proxy cannot be null");
        if (!Proxy.Type.DIRECT.equals(proxy.type())) this.proxy = proxy;
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

    /**
     * Call this method before creating and instance of the factory if you want to use a proxy
     *
     * @throws NoRouteToHostException if there is no network access to a test host. (user behind a firewall or using a proxy)
     */
    public void setupProxy() throws NoRouteToHostException {
        //1. Check if there is direct internet access
        if (testDirectAccess()) return;

        //2. try the local proxy if available
        final URL url;
        try {
            url = new URL(ProxyUtil.DEFAULT_HTTP_TEST_URL);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid URL: " + ProxyUtil.DEFAULT_HTTP_TEST_URL, e);
        }
        if (testProxyAccess(url)) return;

        //3. try to auto detect the proxy
        if (detectProxy(url)) return;

        // We should not reach this statement if we are able to get through
        throw new NoRouteToHostException("Failed to connect to host: " + ProxyUtil.DEFAULT_HTTP_TEST_URL);
    }

    private boolean testDirectAccess() {
        return ProxyUtil.testInternetAccess();
    }

    private boolean testProxyAccess(final URL url) {
        return proxy != null && ProxyUtil.testInternetAccess(proxy, url);
    }

    private boolean detectProxy(final URL url) throws NoRouteToHostException {
        final List<Proxy> proxies = ProxyUtil.getProxy();
        if (CollectionUtil.isNullOrEmpty(proxies)) return false;

        for (Proxy proxy : proxies) {
            // test for http
            if (ProxyUtil.testInternetAccess(proxy, url)) {
                this.proxy = proxy;
                return true;
            }
        }

        return false;
    }

    private ProxyServer getProxyServer() {
        if (proxy == null || proxy.type() == null || Proxy.Type.DIRECT.equals(proxy.type())
                || proxy.address() == null || !(proxy.address() instanceof InetSocketAddress)) return null;

        final InetSocketAddress address = (InetSocketAddress) proxy.address();
        return new ProxyServer(address.getHostName(), address.getPort());
    }
}
