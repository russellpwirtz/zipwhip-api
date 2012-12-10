package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.DefaultZipwhipClient;
import com.zipwhip.api.HttpApiConnectionFactory;
import com.zipwhip.api.ZipwhipClient;
import com.zipwhip.api.settings.MemorySettingStore;
import com.zipwhip.api.signals.SocketSignalProviderFactory;
import com.zipwhip.api.signals.reconnect.DefaultReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.RawSocketIoChannelPipelineFactory;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.reliable.retry.ExponentialBackoffRetryStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/11/12
 * Time: 7:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ZipwhipClientIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketSignalProviderIntegrationTest.class);
    private String sessionKey = "fc3890ba-a2c7-4449-a4c7-c80f57af228b:142584301"; // evo 3d

    ZipwhipClient zipwhipClient;

    @Before
    public void setUp() throws Exception {
        HttpApiConnectionFactory connectionFactory = new HttpApiConnectionFactory();

        connectionFactory.setHost(ApiConnection.STAGING_HOST);
        connectionFactory.setSessionKey(sessionKey);

        SocketSignalProviderFactory signalProviderFactory = SocketSignalProviderFactory.newInstance()
                .address(new InetSocketAddress(ApiConnection.STAGING_SIGNALS_HOST, ApiConnection.DEFAULT_SIGNALS_PORT))
                .reconnectStrategy(new DefaultReconnectStrategy(null, new ExponentialBackoffRetryStrategy(1000, 2.0)))
                .channelPipelineFactory(new RawSocketIoChannelPipelineFactory(60, 5));

        DefaultZipwhipClient client = new DefaultZipwhipClient(null, null, null, connectionFactory.create(), signalProviderFactory.create());
        client.setSettingsStore(new MemorySettingStore());

        zipwhipClient = client;
    }

    @Ignore
    @Test
    public void testConnect() throws Exception {
        TestUtil.awaitAndAssertSuccess(zipwhipClient.connect());
    }

    @Ignore
    @Test
    public void testConnectDisconnect() throws Exception {
        TestUtil.awaitAndAssertSuccess(zipwhipClient.connect());
        TestUtil.awaitAndAssertSuccess(zipwhipClient.disconnect());
    }

    @Ignore
    @Test
    public void testConnectDisconnectConnect() throws Exception {
        TestUtil.awaitAndAssertSuccess(zipwhipClient.connect());
        TestUtil.awaitAndAssertSuccess(zipwhipClient.disconnect());
        TestUtil.awaitAndAssertSuccess(zipwhipClient.connect());
    }

    @After
    public void tearDown() throws Exception {
        zipwhipClient.destroy();
    }
}
