package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.ChannelStateManagerFactory;
import com.zipwhip.api.signals.sockets.StateManager;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import java.net.SocketAddress;
import java.util.concurrent.*;

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
    private SignalConnectionDelegate delegate;

    /**
     *
     */
    private StateManager<ChannelState> stateManager;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * For keeping absolute care over the thread safe state
     */
    public ChannelWrapper(Channel channel, SignalConnectionDelegate delegate) {
        this.channel = channel;
        this.delegate = delegate;

        this.stateManager = ChannelStateManagerFactory.newStateManager();
    }

    /**
     *
     * @param remoteAddress
     * @throws Exception if not successful
     */
    public boolean connect(final SocketAddress remoteAddress) throws InterruptedException {

        stateManager.transitionOrThrow(ChannelState.CONNECTING);
        assert (!isConnected());

        // do the connect async.
        ChannelFuture future = channel.connect(remoteAddress);
        boolean completed;

        try {
            // TODO: What exception does it throw if it's a bad endpoint?
            // since we're on the IO thread, we need to block here.
            completed = future.await(SignalConnectionBase.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            if (!future.isDone()) {
                // shit it didn't finish!
                // cancel it.
                future.cancel();
                // Subsequent calls to close have no effect
                future.getChannel().close();
            }

            stateManager.transitionOrThrow(ChannelState.DISCONNECTED);

            throw e;
        }

        if (!completed) {
            // shit it didn't finish!
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
    public void disconnect() {

        stateManager.transitionOrThrow(ChannelState.DISCONNECTING);
        assert (isConnected());
        assert (!isConnecting());
        assert (!isDisconnected());

        // because we intend to delete this.channel during the 'ondestroy' method, we need to capture the
        // reference so we don't NPE.
        Channel channel = this.channel;

        LOGGER.debug("Closing channel " + channel);

        // the delegate needs to know that it's been destroyed.
        // the word 'destroy' here means that it is no longer the currently active channel.
        // this is important to prevent stray requests from entering the queues from the network channel threads.
        synchronized (this) {
            this.channel = null;
        }

        this.delegate.destroy();

        try {
            // wait synchronously for it to close?
            if (!channel.close().await(SignalConnectionBase.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)){
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
        } catch (InterruptedException e) {
            // it was interrupted, do we call this disconnected??
            stateManager.transitionOrThrow(ChannelState.DISCONNECTED);
        }
    }


    public synchronized Future<Boolean> write(Object message) {
        assertConnected();

        return FutureUtil.execute(executor, new WriteOnChannelSafelyCallable(this, message));
    }

    private void assertConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Not currently connected");
        }

        stateManager.ensure(ChannelState.CONNECTED);
    }


    protected boolean isConnecting() {
        if (channel == null){
            return false;
        }

        return (channel.isOpen() || channel.isBound()) && !channel.isConnected();
    }

    protected boolean isDisconnected() {
        if (channel == null){
            return false;
        }

        return channel.getCloseFuture().isDone() && channel.getCloseFuture().isSuccess();
    }

    // we can't synchronize on this object because other threads are trying to peer in.
    protected synchronized boolean isConnected() {
        if (channel == null){
            return false;
        }

        return channel.isConnected() && channel.isBound() && channel.isOpen() && channel.isReadable() && channel.isWritable();
    }

    private void assertClosed(Channel channel) {
        // double check that it worked?
        if (isConnected()) {
            throw new IllegalStateException(String.format("We assumed that Netty made isOpen, isConnected, isBound false! %b, %b, %b", channel.isOpen(), channel.isConnected(), channel.isBound()));
        }
    }

    @Override
    protected void onDestroy() {
        // ref counting?
        this.channel = null;
        // forcibly kill it.
        if (!delegate.isDestroyed()){
            delegate.destroy();
        }
    }
}
