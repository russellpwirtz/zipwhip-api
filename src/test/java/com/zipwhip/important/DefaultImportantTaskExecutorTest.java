package com.zipwhip.important;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.important.connect.ConnectCommandTask;
import com.zipwhip.api.signals.important.connect.ConnectCommandWorker;
import com.zipwhip.api.signals.sockets.netty.NettySignalConnection;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.important.tasks.SimpleImportantTask;
import com.zipwhip.important.workers.AlwaysSucceedWorker;
import com.zipwhip.util.DateUtil;
import org.junit.Before;
import org.junit.Test;

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

        importantTaskExecutor.register("test", new AlwaysSucceedWorker());

        this.importantTaskExecutor = importantTaskExecutor;
    }

    @Test
    public void testExecuteNow() throws Exception {
        // the SimpleTaskRequest is synchronous
        ObservableFuture future = importantTaskExecutor.enqueue(new SimpleImportantTask("test", null));

        assertTrue(future.isSuccess());
    }

    @Test
    public void testActionConnectResponse() throws Throwable {

        connection = new NettySignalConnection();

        ((ImportantTaskExecutor) importantTaskExecutor).register("connect", new ConnectCommandWorker(connection));

        connection.connect().get(5, TimeUnit.SECONDS);

        ObservableFuture<String> future = importantTaskExecutor.enqueue(new ConnectCommandTask(null));

        // actually it could happen really damn fast.
//        assertFalse(future.isDone());

        future.await(5, TimeUnit.SECONDS);

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

        ((ImportantTaskExecutor) importantTaskExecutor).register("connect", new ConnectCommandWorker(connection));

        ObservableFuture<String> future = importantTaskExecutor.enqueue(new ConnectCommandTask(null, DateUtil.inFuture(1, TimeUnit.SECONDS)));

        boolean finished = future.await(5, TimeUnit.SECONDS);

        assertTrue("Didn't finish like expected!", finished);

        assertTrue(future.isDone());
        assertFalse(future.isSuccess());
        assertNotNull(future.getCause());
        assertFalse(future.isCancelled());
    }
}
