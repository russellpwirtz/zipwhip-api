/**
 * 
 */
package com.zipwhip.api.signals.reconnect;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.MockSignalConnection;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * @author jdinsel
 *
 */
public class ExponentialBackoffReconnectStrategyTest {

	private ExponentialBackoffReconnectStrategy strategy;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		strategy = new ExponentialBackoffReconnectStrategy();
		strategy.setSignalConnection(new CannotConnectSignalConnection());
		strategy.setDelayUnits(TimeUnit.MILLISECONDS);
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@Test
	public void test() throws InterruptedException {

		long backoff = -1;
		for (int i = 1; i < 20; i++) {
			strategy.setConsecutiveReconnectAttempts(i);
			long delay = strategy.calculateBackoff();
			assertTrue("On iteration " + i + " " + delay + " < " + backoff, delay >= backoff);
			backoff = delay;
		}


	}

	private class CannotConnectSignalConnection extends MockSignalConnection {

			@Override
		public synchronized ObservableFuture<ConnectionHandle> connect() {
            return new FakeFailingObservableFuture<ConnectionHandle>(this, new Exception());
		}
	}
}
