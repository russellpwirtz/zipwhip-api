package com.zipwhip.api;

import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.api.signals.reconnect.ExponentialBackoffReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.api.signals.sockets.SocketSignalProvider;
import com.zipwhip.api.signals.sockets.netty.RawSocketIoChannelPipelineFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.DownloadURL;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.zipwhip.util.Asserts.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/7/12
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class JProfilerTest1 {

    private static final Logger LOGGER = Logger.getLogger(JProfilerTest1.class);

    ZipwhipClient client;
    SocketSignalProvider signalProvider;

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();

        JProfilerTest1 t = new JProfilerTest1();
        t.setUp();
        while (true) {
            LOGGER.debug("==== RUNNING TEST ====");
            t.runTest();
            LOGGER.debug("Sleeping for 10 seconds...");
            Thread.sleep(100);
        }
    }

    public void setUp() throws Exception {
        ApiConnectionConfiguration.API_HOST = ApiConnection.STAGING_HOST;
        ApiConnectionConfiguration.SIGNALS_HOST = ApiConnection.STAGING_SIGNALS_HOST;

        ApiConnectionFactory connectionFactory = ApiConnectionFactory.newInstance();
//                .sessionKey(sessionKey);

//        connectionFactory.setUsername("9139802972");
//        connectionFactory.setPassword("asdfasdf");

        // staging
        connectionFactory.setPassword("pistons456");
        connectionFactory.setUsername("2062513225");

        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance()
                .reconnectStrategy(new ExponentialBackoffReconnectStrategy())
                .channelPipelineFactory(new RawSocketIoChannelPipelineFactory(60, 5));

        ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

        ZipwhipClient zipwhipClient = zipwhipClientFactory.create();

        zipwhipClient.setSettingsStore(new MemorySettingStore());

        client = zipwhipClient;
        signalProvider = (SocketSignalProvider) client.getSignalProvider();
    }

    public void runTest() throws Exception {
        ObservableFuture<ConnectionHandle> future = client.connect();

        future.await();

        Asserts.assertTrue(future.isSuccess(), "Not successful?");

        ConnectionHandle connectionHandle = future.getResult();

        assertTrue(!connectionHandle.isDestroyed(), "Not destroyed");

        String sessionKey = client.getConnection().getSessionKey();

//        for (int i = 0; i < 100; i++) {
//            Thread.sleep(100);
//            final String requestId = UUID.randomUUID().toString();
//            int index = new Random().nextInt();
//
//            final CountDownLatch latch = new CountDownLatch(1);
//
//            final Signal[] verifySignal = new Signal[1];
//
//            client.getSignalProvider().getSignalReceivedEvent().addObserver(new Observer<List<Signal>>() {
//                @Override
//                public void notify(Object sender, List<Signal> item) {
//                    for (Signal signal : item) {
//                        if (requestId.equals(signal.getType())) {
//                            verifySignal[0] = signal;
//                            assertNotNull(signal);
//                        }
//                    }
//                    latch.countDown();
//                }
//            });
//
//            LOGGER.debug(DownloadURL.get("http://staging.zipwhip.com/mvc/signals/signal?session=" + sessionKey + "&requestId=" + requestId + "&type=" + requestId + "&scope=" + index));
//
//            assertTrue(latch.await(50, TimeUnit.SECONDS), "Latch didn't finish?");
//
//            assertNotNull(verifySignal[0]);
//        }

        client.disconnect().await();
    }

    private void assertNotNull(Signal object) {
        if (object == null) {
            throw new NullPointerException();
        }
    }

}
