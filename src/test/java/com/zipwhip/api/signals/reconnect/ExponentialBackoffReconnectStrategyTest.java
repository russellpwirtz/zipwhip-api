/**
 * 
 */
package com.zipwhip.api.signals.reconnect;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.zipwhip.api.signals.sockets.MockSignalConnection;

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
		BasicConfigurator.configure();
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
		public synchronized Future<Boolean> connect() throws Exception {

			FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					isConnected = false;

					System.out.println("Forcing a failed connect");
					return Boolean.valueOf(isConnected);
				}
			});

			if (executor == null) {
				executor = Executors.newSingleThreadExecutor();
			}
			executor.execute(task);

			return task;
		}
	}
}
