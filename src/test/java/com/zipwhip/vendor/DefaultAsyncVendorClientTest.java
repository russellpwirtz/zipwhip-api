package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ApiConnectionFactory;
import com.zipwhip.api.NingHttpConnection;
import com.zipwhip.concurrent.NetworkFuture;
import com.zipwhip.events.MockObserver;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.util.SignTool;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/10/11
 * Time: 4:59 PM
 */
public class DefaultAsyncVendorClientTest {

    String apiKey = "18adc2";
    String secret = "249fasdasdff5cc-5aasdfdf2-4707-9f5e-da1893b92e851dfb4ee0-4156-469b-8ee3-d50912";

    MockObserver<NetworkFuture<Boolean>> observer = new MockObserver<NetworkFuture<Boolean>>();

    @Test
    public void testAuthentication() throws Exception {

        BasicConfigurator.configure();

//        Executor workerExecutor = new SimpleExecutor();
//        Executor mainExecutor = new SimpleExecutor();
//        SignTool signTool = new SignTool(apiKey, secret);

//        ApiConnection connection = new NingHttpConnection(mainExecutor, workerExecutor, signTool);
//        ApiConnectionFactory factory = new ApiConnectionFactory(connection);
//        factory.create();
//
//        AsyncVendorClient client = new DefaultAsyncVendorClient(connection);

//        AsyncVendorClient client = AsyncVendorClientFactory.createViaApiKey(apiKey, secret);
//
//        NetworkFuture<Boolean> future = client.userSubscribe("", null);
//
//        future.addObserver(observer);
//
//        future.awaitUninterruptibly();
//
//        assertTrue(future.isDone());
//        assertTrue(future.isSuccess());
//        assertFalse(future.isCancelled());
//        assertTrue(observer.isCalled());

    }
}
