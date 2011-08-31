package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 3:33 PM
 */
public class MockSignalConnection extends DestroyableBase implements SignalConnection {

    private static final int PING_TIMEOUT = 1000 * 30; // when to ping, inactive seconds
    private static final int PONG_TIMEOUT = 1000 * 30; // when to disconnect if a ping was not ponged by this time

    private ExecutorService executor;

    private ScheduledFuture<?> pingTimeoutFuture;
    private ScheduledExecutorService pingTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> pongTimeoutFuture;
    private ScheduledExecutorService pongTimeoutExecutor = Executors.newSingleThreadScheduledExecutor();

    private ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
    private ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();

    private boolean isConnected = false;

    @Override
    public synchronized Future<Boolean> connect() throws Exception {

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                isConnected = true;
                connectEvent.notifyObservers(this, isConnected);
                receiveEvent.notifyObservers(this, new ConnectCommand("1234-5678-1234-5678", null, null));
                return isConnected;
            }
        });

        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        executor.execute(task);

        return task;
    }

    @Override
    public synchronized Future<Void> disconnect() {

        FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                executor.shutdownNow();
                executor = null;

                if (pingTimeoutFuture != null) {
                    pingTimeoutFuture.cancel(true);
                }

                if (pongTimeoutFuture != null) {
                    pongTimeoutFuture.cancel(true);
                }

                connectEvent.notifyObservers(this, false);

                return null;
            }
        });

        executor.execute(task);
        return task;
    }

    @Override
    public void send(SerializingCommand command) {

    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onMessageReceived(Observer<Command> observer) {
        receiveEvent.addObserver(observer);
    }

    @Override
    public void onConnectionStateChanged(Observer<Boolean> observer) {
        connectEvent.addObserver(observer);
    }

    @Override
    public void setHost(String host) {
    }

    @Override
    public void setPort(int port) {
    }

    @Override
    protected void onDestroy() {

        if (this.isConnected()) {
            this.disconnect();
        }

        pingTimeoutFuture.cancel(true);
        pongTimeoutFuture.cancel(true);

        pongTimeoutExecutor.shutdownNow();
        pongTimeoutExecutor.shutdownNow();
    }

    private void schedulePing() {

        if (!pingTimeoutExecutor.isShutdown() && !pingTimeoutExecutor.isTerminated()) {

            if (pingTimeoutFuture != null) {
                pingTimeoutFuture.cancel(false);
            }
        }

        pingTimeoutFuture = pingTimeoutExecutor.schedule(new Runnable() {
            @Override
            public void run() {

                send(PingPongCommand.getInstance());

                pongTimeoutFuture = pongTimeoutExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        disconnect();
                    }
                }, PONG_TIMEOUT, TimeUnit.MILLISECONDS);

            }
        }, PING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

}
