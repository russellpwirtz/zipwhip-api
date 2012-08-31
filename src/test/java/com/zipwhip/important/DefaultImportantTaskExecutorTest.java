package com.zipwhip.important;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.sockets.ConnectCommandTask;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.FakeObservableFuture;
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

    //    private String sessionKey = "c821c96c-39fd-49ad-b9d4-b71d0d14f6ae:375"; // evo 3d
    private String sessionKey = "6c20b056-6843-404d-9fb4-b492d54efe75:142584301"; // evo 3d

    private ImportantTaskExecutor importantTaskExecutor;
    private SignalConnection connection;

    @Before
    public void setUp() throws Exception {
        ImportantTaskExecutor importantTaskExecutor = new ImportantTaskExecutor();

        this.importantTaskExecutor = importantTaskExecutor;
    }

    @Test
    public void testExecuteNow() throws Exception {
        // the SimpleTaskRequest is synchronous
        ObservableFuture<Boolean> future = importantTaskExecutor.enqueue(new Callable<ObservableFuture<Boolean>>() {
            @Override
            public ObservableFuture<Boolean> call() throws Exception {
                return new FakeObservableFuture<Boolean>(this, true);
            }
        });

        assertTrue(future.isSuccess());
    }

    @Test
    public void testActionConnectResponse() throws Throwable {

        connection = new NettySignalConnection();

        connection.connect().get(5, TimeUnit.SECONDS);

        ObservableFuture<ConnectCommand> future = importantTaskExecutor.enqueue(new ConnectCommandTask(connection, null, null, null));

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
        connection = new NettySignalConnection() {
            @Override
            public synchronized ObservableFuture<Boolean> send(SerializingCommand command) throws IllegalStateException {
                // FAKE THE SEND.
                return new DefaultObservableFuture<Boolean>(this);
            }
        };

        ObservableFuture<ConnectCommand> future = importantTaskExecutor.enqueue(new ConnectCommandTask(connection, null, null, null), FutureDateUtil.in1Second());

        boolean finished = future.await(10, TimeUnit.SECONDS);

        assertTrue("Didn't finish like expected!", finished);

        assertTrue(future.isDone());
        assertFalse(future.isSuccess());
        assertNotNull(future.getCause());
        assertFalse(future.isCancelled());
    }
}
