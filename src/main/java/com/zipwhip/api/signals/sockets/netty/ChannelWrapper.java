package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.ChannelStateManagerFactory;
import com.zipwhip.api.signals.sockets.StateManager;
import com.zipwhip.concurrent.Futures;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.Destroyable;
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
     * For keeping absolute care over the thread safe state
     */
    public ChannelWrapper(Channel channel, SignalConnectionDelegate delegate) {
        this.channel = channel;
        this.delegate = delegate;
    }

    public synchronized void connect(final SocketAddress remoteAddress) throws Exception {
        if (isConnected()) {
            throw new IllegalStateException("Currently connected! Cannot connect again.");
        }

        // do the connect async.
        ChannelFuture future = channel.connect(remoteAddress);

        // since we're on the IO thread, we need to block here.
        boolean completed = future.await(SignalConnectionBase.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!completed) {
            // shit it didn't finish!
            // cancel it.
            future.cancel();
        }

        assert isConnected();
    }

    /**
     * Will execute the connection/disconnection on a single common thread.
     *
     * @param networkDisconnect
     * @return
     */
    public synchronized void disconnect(final boolean networkDisconnect) throws InterruptedException {

        if (isConnected()) {
            throw new IllegalStateException("The channel is already disconnected");
        }

        LOGGER.debug("Closing channel " + channel);

        // wait synchronously for it to close?
        ChannelFuture closeFuture = channel.close().await();

        if (closeFuture.isSuccess()) {
            LOGGER.debug("Closing channel SUCCESS " + channel);
        } else {
            LOGGER.debug("Closing channel FAILURE  " + channel);
        }

        // the delegate needs to know that it's been destroyed.
        // the word 'destroy' here means that it is no longer the currently active channel.
        // this is important to prevent stray requests from entering the queues from the network channel threads.
        delegate.destroy();

        // kill the current wrapper.
        destroy();

        // for debugging (do after all the destroys took place so we aren't left in a bad state after the exception)
        assertClosed(closeFuture.getChannel());
    }

    protected synchronized boolean isConnected() {
        return channel.isConnected() && channel.isBound() && channel.isOpen() && channel.isReadable() && channel.isWritable();
    }

    private void assertClosed(Channel channel) {
        // double check that it worked?
        if (!isConnected()) {
            throw new IllegalStateException(String.format("We assumed that Netty made isOpen, isConnected, isBound false! %b, %b, %b", channel.isOpen(), channel.isConnected(), channel.isBound()));
        }
    }

    @Override
    protected void onDestroy() {
        // forcibly kill it.
    }
}
