package com.zipwhip.api.signals;

import com.zipwhip.concurrent.ObservableFuture;
import org.junit.Before;
import org.junit.Test;

/**
 * Date: 8/7/13
 * Time: 3:11 PM
 *
 * @author Michael
 * @version 1
 */
public class SignalProviderTest {

    SignalProviderImpl signalProvider;

    @Before
    public void setUp() throws Exception {
        signalProvider = new SignalProviderImpl();
    }

    @Test
    public void testConnect() throws Exception {
        ObservableFuture<Void> future = signalProvider.connect();

//        assertTrue(future.await());

    }
}
