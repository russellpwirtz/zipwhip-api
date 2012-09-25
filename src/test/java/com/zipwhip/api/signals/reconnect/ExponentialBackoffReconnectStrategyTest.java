/**
 * 
 */
package com.zipwhip.api.signals.reconnect;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.MockSignalConnection;
import com.zipwhip.concurrent.FakeFailingObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.reliable.retry.ExponentialBackoffRetryStrategy;
import com.zipwhip.util.Factory;
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

    private ExponentialBackoffRetryStrategy retryStrategy;
//	private DefaultReconnectStrategy strategy;

    /**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
        retryStrategy = new ExponentialBackoffRetryStrategy(1000, 2.0);
//        strategy = new DefaultReconnectStrategy(null, retryStrategy);
//		strategy.setSignalConnection(new CannotConnectSignalConnection());
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@Test
	public void test() throws InterruptedException {

		long backoff = -1;
		for (int i = 1; i < 20; i++) {
			long delay = retryStrategy.getNextRetryInterval(i);
			assertTrue("On iteration " + i + " " + delay + " < " + backoff, delay >= backoff);
			backoff = delay;
		}


	}
//
//    private class CannotConnectSignalConnection extends MockSignalConnection {
//
//        public CannotConnectSignalConnection(Factory<ExecutorService> executorFactory) {
//            super(executorFactory.create());
//        }
//
//        public CannotConnectSignalConnection() {
//            super(SimpleExecutor.getInstance());
//        }
//
//        @Override
//        public synchronized ObservableFuture<ConnectionHandle> connect() {
//            return new FakeFailingObservableFuture<ConnectionHandle>(this, new Exception());
//        }
//    }
}
