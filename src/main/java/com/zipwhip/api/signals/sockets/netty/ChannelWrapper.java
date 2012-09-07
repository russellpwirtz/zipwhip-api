package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionStateManagerFactory;
import com.zipwhip.util.StateManager;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Asserts;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/15/12
 * Time: 2:47 PM
 * <p/>
 * This wrapper controls the access to a channel. The concept is that 1 wrapper controls all access to 1 channel.
 * Since all requests to a channel must go through this wrapper, we can safely control the threading behavior.
 *
 * THIS WRAPPER IS NEVER ALLOWED TO SELF DESTRUCT!
 */
public class ChannelWrapper extends CascadingDestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(ChannelWrapper.class);

    /**
     * This delegate represents the channel's access to our SignalConnectionBase class. The ChannelHandlers
     * need to be able to talk into the SignalProvider 'safely'. If their connection gets torn down we need to
     * terminate their access to the provider.
     */
    private SignalConnectionDelegate delegate;

    /**
     * This is the channel that we're trying to protect.
     */
    protected Channel channel;

    /**
     * This connection is our external representation of the connection. External entities can interact with it
     * through this 'connection' object. It's very similar to the "delegate" though I was concerned about combining
     * them since they have different purposes and access.
     */
    protected final ChannelWrapperConnectionHandle connection;

    /**
     * Independently keep track of the state of the connection.
     */
    protected final StateManager<ConnectionState> stateManager;
    protected final ExecutorService executor;

    /**
     * For keeping absolute care over the thread safe state
     */
    public ChannelWrapper(long id, Channel channel, SignalConnectionDelegate delegate, ExecutorService executor) {
        this.channel = channel;
        this.delegate = delegate;
        this.stateManager = ConnectionStateManagerFactory.newStateManager();
        this.link(stateManager);
        this.connection = new ChannelWrapperConnectionHandle(id, delegate.signalConnectionBase, this);
        this.connection.link(this);
        if (executor == null){
            this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("ChannelWrapper-"));
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ChannelWrapper.this.executor.shutdownNow();
                }
            });
        } else {
            this.executor = executor;
        }
    }

    /**
     * Safely connect the underlying channel.
     *
     * @param remoteAddress The address to connect to.
     * @throws InterruptedException if interrupted while connecting.
     */
    public synchronized void connect(final SocketAddress remoteAddress) throws Throwable {

        Asserts.assertTrue(!isDestroyed(), "I was destroyed?");
        Asserts.assertTrue(channel != null, "Channel was destroyed?");

        stateManager.transitionOrThrow(ConnectionState.CONNECTING);

        delegate.pause();
        // do the connect async.
        ChannelFuture future = null;
        boolean completed;

        try {
            future = channel.connect(remoteAddress);
            // since we're on the IO thread, we need to block here.
            completed = future.await(delegate.getConnectTimeoutSeconds(), TimeUnit.SECONDS);

            delegate.resume();
        } catch (Exception e) {
            if (future == null){
                // oh shit! it crashed!
                // Subsequent calls to close have no effect
            } else if (!future.isDone()) {
                future.cancel();
                // Subsequent calls to close have no effect
                future.getChannel().close();
            }

            stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);

            throw e;
        }

        if (!completed) {
            LOGGER.debug("Oh shit, it's not completed. We're going to cancel/close everything.");
            // cancel it.
            future.cancel();
            // Subsequent calls to close have no effect
            future.getChannel().close();
        }

        if (future.isSuccess()) {
            stateManager.transitionOrThrow(ConnectionState.CONNECTED);
        } else {
            stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);
        }

        if (!future.isSuccess()) {
            throw future.getCause();
        }
    }

    /**
     * Will execute the connection/disconnection on a the caller's thread.
     * We will terminate the delegate before attempting to close the connection, so this means that
     * NO netty disconnected events will be able to fire. So the caller should be sure to throw its own
     * appropriate disconnect events rather than rely on the netty ones. NOTE: the netty ones are pretty unreliable
     * anyway.
     */
    public synchronized void disconnect() {

        // make sure our state is correct
        stateManager.transitionOrThrow(ConnectionState.DISCONNECTING);

        // because we intend to delete this.channel during the 'ondestroy' method, we need to capture the
        // reference so we don't NPE.
        final Channel channel = this.channel;

        LOGGER.debug("Closing channel " + channel);

        // the delegate needs to know that it's been destroyed.
        // the word 'destroy' here means that it is no longer the currently active channel.
        // this is important to prevent stray requests from entering the queues from the network channel threads.
        this.channel = null;

        // before we actually close the channel, we need to tear down the link.
        this.delegate.destroy();

        try {
            try {
                // wait synchronously for it to close?
                if (!channel.close().await(delegate.getConnectTimeoutSeconds(), TimeUnit.SECONDS)){
                    // not completed in that time!
                    // what do we do?!?!
                    LOGGER.warn(String.format("The channel %s failed to close in the time limit! We are going to just destroy ourselves anyway.", channel));

                    // TODO: should we just say it's closed anyway??

                    stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);
                } else {
                    LOGGER.debug(String.format("The channel %s closed cleanly", channel));

                    stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);

                    // for debugging (do after all the destroys took place so we aren't left in a bad state after the exception)
                    assertClosed(channel);
                }
            } catch (Exception e) {
                LOGGER.error("Got exception trying to close", e);
                // it was interrupted, do we call this disconnected??
                stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);
            }
        } catch (Exception e){
            LOGGER.error("Crazy, we got an error the catch block of an error! ", e);
        }
    }

    public ObservableFuture<Boolean> write(Object message) {
        stateManager.ensure(ConnectionState.CONNECTED);

        return FutureUtil.execute(executor, null,
                new WriteOnChannelSafelyCallable(this, message));
    }

    public boolean shouldBeConnected() {
        synchronized (stateManager) {
            return stateManager.get() == ConnectionState.CONNECTED;
        }
    }

    /**
     * Safely check if the underlying channel is connected.
     * We can't synchronize on this object because other threads are trying to peer in.
     *
     * @return True if the underlying channel is connected.
     */
    protected ConnectionState getState() {
        return stateManager.get();
    }

    private void assertClosed(Channel channel) {
        // double check that it worked?
        if (channel.isConnected()) {
            throw new IllegalStateException(String.format("We assumed that Netty made isOpen, isConnected, isBound false! %b, %b, %b", channel.isOpen(), channel.isConnected(), channel.isBound()));
        }
    }

    public ConnectionHandle getConnection() {
        return connection;
    }

    @Override
    protected void onDestroy() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Destroying ChannelWrapper %s / %s", this.channel, Thread.currentThread().toString()));
        }

        // ref counting
        this.channel = null;

        // forcibly kill it.
        if (!delegate.isDestroyed()){
            delegate.destroy();
        }

        executor.shutdownNow();

        // dont null out the "connection" object because it would cause null pointers. It's linked so it will auto destroy.
    }
}
