package com.zipwhip.api.signals;

import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 6/6/12
 * Time: 11:36 AM
 */
public class SocketSignalProviderFactoryTest {

    /**
     * This just texts that the reflection works when we have a version of Netty that is not the Zipwhip custom.
     */
    @Test
    public void testOnSocketActivityDeprecation() throws Exception {

        SocketSignalProviderFactory factory = SocketSignalProviderFactory.newInstance();

        factory.onSocketActivity(new Runnable() {
            @Override
            public void run() {
            }
        });

        SignalProvider provider = factory.create();
        provider.connect();
    }

}
