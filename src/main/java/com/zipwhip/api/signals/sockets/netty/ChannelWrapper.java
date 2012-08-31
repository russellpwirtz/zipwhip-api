package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.ChannelStateManagerFactory;
import com.zipwhip.api.signals.sockets.StateManager;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.util.Asserts;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import java.net.SocketAddress;
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
 * THIS WRAPPER IS NEVER ALLOWED TO SELF DESTRUCT.
 *
 */
public class ChannelWrapper extends CascadingDestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(ChannelWrapper.class);

    /**
     * This is the channel that we're trying to protect.
     */
    protected Channel channel;

    /**
     * This delegate represents the channel's access to our SignalConnectionBase class.
     */
    private final SignalConnectionDelegate delegate;

    /**
     * Independently keep track of the state of the connection.
     */
    protected final StateManager<ChannelState> stateManager;
    protected final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("ChannelWrapper-"));
    private SignalConnectionBase connection;

    /**
     * For keeping absolute care over the thread safe state
     */
    public ChannelWrapper(Channel channel, SignalConnectionDelegate delegate) {
        this.channel = channel;
        this.delegate = delegate;
        this.stateManager = ChannelStateManagerFactory.newStateManager();
    }

    /**
     * Safely connect the underlying channel.
     *
     * @param remoteAddress The address to connect to.
     * @throws InterruptedException if interrupted while connecting.
     */
    public synchronized boolean connect(final SocketAddress remoteAddress) throws InterruptedException {

        stateManager.transitionOrThrow(ChannelState.CONNECTING);
        Asserts.assertTrue(!isConnected(), "Channel is not connecting");

        delegate.pause();
        // do the connect async.
        ChannelFuture future = channel.connect(remoteAddress);
        boolean completed;

        try {
            // since we're on the IO thread, we need to block here.
            completed = future.await(delegate.getConnectTimeoutSeconds(), TimeUnit.SECONDS);

            delegate.resume();
        } catch (InterruptedException e) {
            if (!future.isDone()) {
                future.cancel();
                // Subsequent calls to close have no effect
                future.getChannel().close();
            }

            stateManager.transitionOrThrow(ChannelState.DISCONNECTED);

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
            stateManager.transitionOrThrow(ChannelState.CONNECTED);
        } else {
            stateManager.transitionOrThrow(ChannelState.DISCONNECTED);
        }

        return future.isSuccess();
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
        stateManager.transitionOrThrow(ChannelState.DISCONNECTING);

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

                    stateManager.transitionOrThrow(ChannelState.DISCONNECTED);
                } else {
                    LOGGER.debug(String.format("The channel %s closed cleanly", channel));

                    stateManager.transitionOrThrow(ChannelState.DISCONNECTED);

                    // for debugging (do after all the destroys took place so we aren't left in a bad state after the exception)
                    assertClosed(channel);
                }
            } catch (Exception e) {
                LOGGER.error("Got exception trying to close", e);
                // it was interrupted, do we call this disconnected??
                stateManager.transitionOrThrow(ChannelState.DISCONNECTED);
            }
        } catch (Exception e){
            LOGGER.error("Crazy, we got an error the catch block of an error! ", e);
        }
    }

    public synchronized ObservableFuture<Boolean> write(Object message) {
        assertConnected();

        return FutureUtil.execute(executor, null,
                new WriteOnChannelSafelyCallable(this, message));
    }

    public boolean shouldBeConnected() {
        synchronized (stateManager) {
            return stateManager.get() == ChannelState.CONNECTED;
        }
    }

    private void assertConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Not currently connected");
        }

        stateManager.ensure(ChannelState.CONNECTED);
    }

    /**
     * Safely check if the underlying channel is connecting meaning that it is open or bound but not connected.
     * We can't synchronize on this object because other threads are trying to peer in.
     *
     * @return True if the underlying channel is disconnected.
     */
    protected boolean isConnecting() {
        return (channel != null) && channel.isOpen() && !channel.isConnected();
    }

    /**
     * Safely check if the underlying channel is disconnected.
     * We can't synchronize on this object because other threads are trying to peer in.
     *
     * @return True if the underlying channel is disconnected.
     */
    protected synchronized boolean isDisconnected() {
        return (channel != null) && !channel.isConnected();
    }

    /**
     * Safely check if the underlying channel is connected.
     * We can't synchronize on this object because other threads are trying to peer in.
     *
     * @return True if the underlying channel is connected.
     */
    protected boolean isConnected() {
        Channel c = channel;

        return (c != null) && c.isConnected();
    }

    private void assertClosed(Channel channel) {
        // double check that it worked?
        if (isConnected()) {
            throw new IllegalStateException(String.format("We assumed that Netty made isOpen, isConnected, isBound false! %b, %b, %b", channel.isOpen(), channel.isConnected(), channel.isBound()));
        }
    }

    @Override
    protected void onDestroy() {
        LOGGER.debug(String.format("Destroying ChannelWrapper %s / %s", this.channel, Thread.currentThread().toString()));

        // ref counting
        this.channel = null;

        // forcibly kill it.
        if (!delegate.isDestroyed()){
            delegate.destroy();
        }

        executor.shutdownNow();
    }
}
