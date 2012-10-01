package com.zipwhip.api;

import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.api.signals.reconnect.ExponentialBackoffReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.RawSocketIoChannelPipelineFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/31/12
 * Time: 6:19 PM
 */
public class DefaultZipwhipClientIntegrationTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AndroidZipwhipClientFactory.class);

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testConnectExpectingSubscriptionCompleteCommand() throws Exception {
        Factory<ZipwhipClient> factory = new AndroidZipwhipClientFactory();
        ZipwhipClient client = factory.create();

        ObservableFuture future = client.connect();
        TestUtil.awaitAndAssertSuccess(future);

        LOGGER.debug("Connect done!");

        future = client.disconnect();
        TestUtil.awaitAndAssertSuccess(future);

        LOGGER.debug("Disconnect done!");
    }



    private static class AndroidZipwhipClientFactory extends DestroyableBase implements Factory<ZipwhipClient> {

        //    private String host = "http://network.zipwhip.com";
        private String host = ApiConnection.STAGING_HOST;
        //    private String sessionKey = "c821c96c-39fd-49ad-b9d4-b71d0d14f6ae:375"; // evo 3d
        private String sessionKey = "6c20b056-6843-404d-9fb4-b492d54efe75:142584301"; // evo 3d


        @Override
        public ZipwhipClient create() {

            try {
                ApiConnectionFactory connectionFactory = new HttpApiConnectionFactory();
                connectionFactory.setHost(host);
                connectionFactory.setSessionKey(sessionKey);

                SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance()
                        .reconnectStrategy(new ExponentialBackoffReconnectStrategy())
                        .address(new InetSocketAddress(ApiConnection.STAGING_SIGNALS_HOST, ApiConnection.DEFAULT_SIGNALS_PORT))
                        .channelPipelineFactory(new RawSocketIoChannelPipelineFactory(60, 5));

                ZipwhipClientFactory zipwhipClientFactory = new ZipwhipClientFactory(connectionFactory, signalProviderFactory);

                ZipwhipClient zipwhipClient = zipwhipClientFactory.create();

                zipwhipClient.setSettingsStore(new MemorySettingStore());

                return zipwhipClient;
            } catch (Exception e) {
                LOGGER.error("Error creating ZipwhipClient: ", e);
                throw new RuntimeException(e);
            }
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        @Override
        protected void onDestroy() {
        }
    }


}
