package com.zipwhip.important;

import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.FakeObservableFuture;
import com.zipwhip.util.FutureDateUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: Russ
 * Date: 8/29/12
 * Time: 12:45 PM
 */
public class DefaultImportantTaskExecutorTest {

    private ImportantTaskExecutor importantTaskExecutor;

    @Before
    public void setUp() throws Exception {
        ImportantTaskExecutor importantTaskExecutor = new ImportantTaskExecutor();

        this.importantTaskExecutor = importantTaskExecutor;
    }

    @Test
    public void testExecuteNow() throws Exception {
        // the SimpleTaskRequest is synchronous
        ObservableFuture<Boolean> future = importantTaskExecutor.enqueue(null, new Callable<ObservableFuture<Boolean>>() {
            @Override
            public ObservableFuture<Boolean> call() throws Exception {
                return new FakeObservableFuture<Boolean>(this, true);
            }
        });

        assertTrue(future.isSuccess());
    }

    @Test
    public void testActionConnectResponse() throws Throwable {

        ObservableFuture<Boolean> future = importantTaskExecutor.enqueue(null, new TestTask(true));

        // actually it could happen really damn fast.
//        assertFalse(future.isDone());

        if (!future.await(10, TimeUnit.SECONDS)) {
            // i want to pause
            System.out.println("Hmm, it didnt complete in time. Do you have a break point and you're screwing with my futures?");
//            fail("Future didn't complete in time!");
        }

        if (future.isDone() && !future.isSuccess()) {
            throw future.getCause();
        }

        assertTrue(future.isSuccess());
        assertTrue(!future.isCancelled());
    }

    @Test
    public void testActionConnectNeverComesBack() throws Throwable {

        ObservableFuture<Boolean> future = importantTaskExecutor.enqueue(null, new TestTask(true) {
            @Override
            public ObservableFuture<Boolean> call() throws Exception {
                return new DefaultObservableFuture<Boolean>(this);
            }
        }, FutureDateUtil.in1Second());

        boolean finished = future.await(10, TimeUnit.SECONDS);

        assertTrue("Didn't finish like expected!", finished);

        assertTrue(future.isDone());
        assertFalse(future.isSuccess());
        assertNotNull(future.getCause());
        assertFalse(future.isCancelled());
    }


    private class TestTask implements Callable<ObservableFuture<Boolean>> {

        final Boolean result;

        private TestTask(Boolean result) {
            this.result = result;
        }

        @Override
        public ObservableFuture<Boolean> call() throws Exception {
            return new FakeObservableFuture<Boolean>(this, result);
        }
    }
}
