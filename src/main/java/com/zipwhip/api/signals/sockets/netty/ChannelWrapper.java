package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.api.signals.sockets.ConnectionStateManagerFactory;
import com.zipwhip.api.signals.sockets.netty.pipeline.SignalsChannelHandler;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.concurrent.NamedThreadFactory;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.Asserts;
import com.zipwhip.util.StateManager;
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
 * This wrapper controls the ensureAbleTo to a channel. The concept is that 1 wrapper controls all ensureAbleTo to 1 channel.
 * Since all requests to a channel must go through this wrapper, we can safely control the threading behavior.
 *
 * THIS WRAPPER IS NEVER ALLOWED TO SELF DESTRUCT!
 */
public class ChannelWrapper extends CascadingDestroyableBase {

    private static final Logger LOGGER = Logger.getLogger(ChannelWrapper.class);

    /**
     * This delegate represents the channel's ensureAbleTo to our SignalConnectionBase class. The ChannelHandlers
     * need to be able to talk into the SignalProvider 'safely'. If their connection gets torn down we need to
     * terminate their ensureAbleTo to the provider.
     */
    private SignalConnectionDelegate delegate;

    /**
     * This is the channel that we're trying to protect.
     */
    public Channel channel;

    /**
     * This connection is our external representation of the connection. External entities can interact with it
     * through this 'connection' object. It's very similar to the "delegate" though I was concerned about combining
     * them since they have different purposes and ensureAbleTo.
     */
    protected final ChannelWrapperConnectionHandle connection;

    /**
     * Independently keep track of the state of the connection.
     */
    protected final StateManager<ConnectionState> stateManager;
    protected final ExecutorService executor;

    protected final SignalConnectionBase signalConnectionBase;

    /**
     * For keeping absolute care over the thread safe state
     */
    public ChannelWrapper(long id, Channel channel, SignalConnectionBase signalConnectionBase, ExecutorService executor) {
        this.channel = channel;
        this.stateManager = ConnectionStateManagerFactory.newStateManager();
        this.link(stateManager);
        this.connection = new ChannelWrapperConnectionHandle(id, signalConnectionBase, this);
        this.connection.link(this);
        if (executor == null){
            this.executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("ChannelWrapper(newSingleThreadExecutor)-"));
            this.link(new DestroyableBase() {
                @Override
                protected void onDestroy() {
                    ChannelWrapper.this.executor.shutdownNow();
                }
            });
        } else {
            this.executor = executor;
        }
        this.signalConnectionBase = signalConnectionBase;
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

        Asserts.assertTrue(delegate == null, "Did this class get used twice?");
        // do the connect async.
        ChannelFuture future = null;
        boolean completed;

        try {
            LOGGER.debug(String.format("Connecting %s to %s", channel,  remoteAddress));
            channel.getConfig().setConnectTimeoutMillis(signalConnectionBase.getConnectTimeoutSeconds() * 1000);
            future = channel.connect(remoteAddress);

            // since we're on the IO thread, we need to block here.
            completed = future.await(signalConnectionBase.getConnectTimeoutSeconds(), TimeUnit.SECONDS);


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
        } else {
            setupConnectedChannel(channel);
        }

        if (future.isSuccess()) {
            stateManager.transitionOrThrow(ConnectionState.CONNECTED);
        } else {
            stateManager.transitionOrThrow(ConnectionState.DISCONNECTED);
        }

        if (!future.isSuccess()) {
            if (future.getCause() != null) {
                throw future.getCause();
            } else {
                throw new IllegalStateException("The future was not successful " + future);
            }
        }
    }

    private void setupConnectedChannel(Channel channel) {
        // the delegate lets the ChannelHandlers talk to the connection (such as pong-received)
        this.delegate = new SignalConnectionDelegate(this.signalConnectionBase, this.connection);

        // add the 'business logic' ChannelHandler to the pipeline
        channel.getPipeline().addLast("nettyChannelHandler", new SignalsChannelHandler(this.delegate));

        this.link(this.delegate);
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
        if (this.delegate != null)
            this.delegate.pause();

        try {
            try {
                // wait synchronously for it to close?
                if (!channel.close().await(signalConnectionBase.getConnectTimeoutSeconds(), TimeUnit.SECONDS)){
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

    public synchronized ObservableFuture<Boolean> write(Object message) {
        // need to ensure that the CONNECTED state doesn't change.
        stateManager.ensure(ConnectionState.CONNECTED);

        return FutureUtil.execute(executor, null,
                new WriteOnChannelSafelyCallable(this, message));
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
        this.delegate = null;

        executor.shutdownNow();

        // dont null out the "connection" object because it would cause null pointers. It's linked so it will auto destroy.
    }
}
