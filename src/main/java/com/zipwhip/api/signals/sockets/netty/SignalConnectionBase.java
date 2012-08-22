package com.zipwhip.api.signals.sockets.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.util.SocketAddressUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.CascadingDestroyableBase;

/**
 * @author jdinsel
 */
public abstract class SignalConnectionBase extends CascadingDestroyableBase implements SignalConnection {

    protected static final Logger LOGGER = Logger.getLogger(SignalConnectionBase.class);

    private final Object WRAPPER_BEING_TOUCHED_LOCK = new Object();

    // made not final so it can be testable...
    public static int CONNECTION_TIMEOUT_SECONDS = 45;

    private static final int DEFAULT_PING_TIMEOUT = 1000 * 300; // when to ping inactive seconds
    private static final int DEFAULT_PONG_TIMEOUT = 1000 * 30; // when to disconnect if a ping was not ponged by this time

    private String host = "74.209.177.242";
    private int[] ports = new int[]{80, 443, 8080, 3000};

    private int pingTimeout = DEFAULT_PING_TIMEOUT;
    private int pongTimeout = DEFAULT_PONG_TIMEOUT;

    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    private ScheduledFuture<?> pingTimeoutFuture;
    private ScheduledFuture<?> pongTimeoutFuture;
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

    protected final ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    protected final ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
    protected final ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
    protected final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    protected final ObservableHelper<Boolean> disconnectEvent = new ObservableHelper<Boolean>();

    protected ReconnectStrategy reconnectStrategy;

    //    protected Channel channel;
    protected ChannelWrapper wrapper;
    protected ChannelWrapperFactory channelWrapperFactory;
    protected ChannelPipelineFactory channelPipelineFactory;

    private final ChannelFactory channelFactory = new OioClientSocketChannelFactory(Executors.newSingleThreadExecutor());

    protected boolean connecting;
    protected boolean networkDisconnect;
    protected boolean doKeepalives;

    public void init(ReconnectStrategy reconnectStrategy) {

        this.link(pingEvent);
        this.link(receiveEvent);
        this.link(connectEvent);
        this.link(exceptionEvent);
        this.link(disconnectEvent);

        this.setReconnectStrategy(reconnectStrategy);

        this.doKeepalives = true;

        // sanity checks on the config.
        assert channelPipelineFactory != null;
        assert channelFactory != null;

        channelWrapperFactory = new ChannelWrapperFactory(channelPipelineFactory, channelFactory, this);

        // start the reconnectStrategy whenever we do a connect
        this.connectEvent.addObserver(new StartReconnectStrategyObserver(this));
        // stop the reconnectStrategy whenever we disconnect manually.
        this.disconnectEvent.addObserver(new StopReconnectStrategyObserver(this));
    }

    @Override
    public synchronized Future<Boolean> connect() throws Exception {

        // Enforce a single connection. Crash if we're already connected.
        validateNotConnected();

        // immediately/synchronously create the wrapper.
        synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
            this.wrapper = channelWrapperFactory.create();
        }

        // before we do the "lengthy" network call (which could take 30+ seconds, lets set
        // the networkDisconnect value to true. If this value is true, it means we should
        // retry connect because the network was the problem (instead of the user).
        networkDisconnect = true;

        // make sure this guy doesn't come in while we're working with
        // more work requests. Otherwise we'll get cross talk.
        cancelReconnectStrategy();

        return FutureUtil.execute(this.executor, new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {

                // Make sure that nothing changed between the enqueue and the execute.
                validateNotConnected();

                // TODO: Make this pretty. The older API allows for fallback, we no longer support it.
                final InetSocketAddress address = SocketAddressUtil.get(host, ports).iterator().next();

                try {
                    LOGGER.debug("Connecting to " + address);

                    // synchronous connection to endpoint,
                    // will crash if not successful.
                    wrapper.connect(address);
                } catch (InterruptedException e) {
                    LOGGER.warn(String.format("The connection to %s was interrupted", address), e);
                }

                networkDisconnect = !wrapper.isConnected();

                if (!wrapper.isConnected()) {
                    synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                        // timeout?
                        wrapper.destroy();
                        wrapper = null;
                    }
                }

                // rebind the listeners
                startReconnectStrategy();

                // announce the event to the listeners
                connectEvent.notifyObservers(this, isConnected());

                return isConnected();
            }
        });
    }

    @Override
    public synchronized Future<Void> disconnect() {
        return disconnect(false);
    }

    @Override
    public synchronized Future<Void> disconnect(final boolean network) {

        validateConnected();

        networkDisconnect = network;

        cancelPingPongs();

        cancelReconnectStrategy();

        return FutureUtil.execute(executor, new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                validateConnected();

                synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                    // sanity check the state to make sure it hasn't changed by the time we ran.

                    try {
                        wrapper.disconnect();
                    } catch (IllegalStateException e) {
                        // odd, very odd, this shouldn't happen.
                        LOGGER.warn("Error disconnecting, the channel said", e);
                    }

                    wrapper.destroy();
                    wrapper = null;
                }

                if (network) {
                    startReconnectStrategy();
                }

                // TODO: can we notify of disconnect earlier??
                disconnectEvent.notifyObservers(this, networkDisconnect);

                return null;
            }

        });
    }

    private void startReconnectStrategy() {
        if (reconnectStrategy != null) {
            reconnectStrategy.start();
        }
    }

    private void validateNotConnected() {
        if ((wrapper != null) && wrapper.isConnected()) {
            throw new IllegalStateException("Tried to connect but we already have a channel connected!");
        }
    }

    private void cancelReconnectStrategy() {
        if (reconnectStrategy != null) {
            reconnectStrategy.stop();
        }
    }

    private void validateConnected() {
        if (wrapper == null || !wrapper.isConnected()) {
            throw new IllegalStateException("Not currently connected, expected to be!");
        }
    }

    private void cancelPingPongs() {
        if (pingTimeoutFuture != null) {
            pingTimeoutFuture.cancel(true);
        }

        if (pongTimeoutFuture != null) {
            pongTimeoutFuture.cancel(true);
        }
    }

    @Override
    public void keepalive() {

        LOGGER.debug("Keepalive requested!");

        cancelPong();
        schedulePing(true);
    }

    @Override
    public void startKeepalives() {

        LOGGER.debug("Start keepalives requested!");

        doKeepalives = true;
        schedulePing(false);
    }

    @Override
    public void stopKeepalives() {

        LOGGER.debug("Start keepalives requested!");

        doKeepalives = false;
        cancelPing();
    }

    @Override
    public synchronized Future<Boolean> send(final SerializingCommand command) throws IllegalStateException {
        // send this over the wire.
        validateConnected();
        assert wrapper.channel.isWritable(); // TODO: consider a special exception?

        return wrapper.write(command);
    }

    @Override
    public synchronized boolean isConnected() {
        if (wrapper == null) {
            return false;
        }

        // sanity check our assumptions.
        assert !wrapper.isDestroyed();

        return wrapper.isConnected();
    }

    @Override
    public void onMessageReceived(Observer<Command> observer) {
        receiveEvent.addObserver(observer);
    }

    @Override
    public void onConnect(Observer<Boolean> observer) {
        connectEvent.addObserver(observer);
    }

    @Override
    public void onDisconnect(Observer<Boolean> observer) {
        disconnectEvent.addObserver(observer);
    }

    @Override
    public void removeOnConnectObserver(Observer<Boolean> observer) {
        connectEvent.removeObserver(observer);
    }

    @Override
    public void removeOnDisconnectObserver(Observer<Boolean> observer) {
        disconnectEvent.removeObserver(observer);
    }

    @Override
    public void onPingEvent(Observer<PingEvent> observer) {
        pingEvent.addObserver(observer);
    }

    @Override
    public void onExceptionCaught(Observer<String> observer) {
        exceptionEvent.addObserver(observer);
    }

    @Override
    public final String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public final int[] getPorts() {
        return ports;
    }

    @Override
    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    @Override
    public int getPingTimeout() {
        return pingTimeout;
    }

    @Override
    public void setPingTimeout(int pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    @Override
    public int getPongTimeout() {
        return pongTimeout;
    }

    @Override
    public void setPongTimeout(int pongTimeout) {
        this.pongTimeout = pongTimeout;
    }

    @Override
    public ReconnectStrategy getReconnectStrategy() {
        return reconnectStrategy;
    }

    @Override
    public void setReconnectStrategy(ReconnectStrategy reconnectStrategy) {

        // Stop our old strategy
        if (this.reconnectStrategy != null) {
            this.reconnectStrategy.stop();
            this.unlink(reconnectStrategy);
        }

        this.reconnectStrategy = reconnectStrategy;
        if (this.reconnectStrategy != null) {
            this.reconnectStrategy.setSignalConnection(this);
            this.link(reconnectStrategy);
        }
    }

    @Override
    protected void onDestroy() {

        if (isConnected()) {
            disconnect();
        }

        if (channelFactory != null) {
            channelFactory.releaseExternalResources();
        }

        if (pingTimeoutFuture != null) {
            pingTimeoutFuture.cancel(true);
        }

        if (pongTimeoutFuture != null) {
            pongTimeoutFuture.cancel(true);
        }

        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    protected void schedulePing(boolean now) {

        cancelPing();

        LOGGER.debug("Scheduling a PING to start in " + pingTimeout + "ms");
        pingEvent.notifyObservers(this, PingEvent.PING_SCHEDULED);

        pingTimeoutFuture = scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {

                LOGGER.debug("Sending a PING");
                pingEvent.notifyObservers(this, PingEvent.PING_SENT);

                // Schedule the timeout first in case there is a problem sending
                // the ping
                pongTimeoutFuture = scheduledExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {

                        LOGGER.warn("PONG timeout, disconnecting...");
                        pingEvent.notifyObservers(this, PingEvent.PONG_TIMEOUT);

                        disconnect(true);
                    }
                }, pongTimeout, TimeUnit.MILLISECONDS);

                send(PingPongCommand.getShortformInstance());

            }
        }, now ? 0 : pingTimeout, TimeUnit.MILLISECONDS);
    }

    protected void receivePong(PingPongCommand command) {

        if (command.isRequest()) {

            LOGGER.debug("Received a REVERSE PING");

            PingPongCommand reversePong = PingPongCommand.getNewLongformInstance();
            reversePong.setTimestamp(command.getTimestamp());
            reversePong.setToken(command.getToken());

            LOGGER.debug("Sending a REVERSE PONG");
            send(reversePong);

        } else {

            LOGGER.debug("Received a PONG");
            pingEvent.notifyObservers(this, PingEvent.PONG_RECEIVED);
        }

        cancelPong();

        if (doKeepalives) {
            schedulePing(false);
        }
    }

    protected void cancelPing() {

        if ((pingTimeoutFuture != null) && !pingTimeoutFuture.isCancelled()) {

            if (pingTimeoutFuture != null) {

                LOGGER.debug("Resetting scheduled PING");
                pingEvent.notifyObservers(this, PingEvent.PING_CANCELLED);

                pingTimeoutFuture.cancel(false);
            }
        }
    }

    protected void cancelPong() {

        if ((pongTimeoutFuture != null) && !pongTimeoutFuture.isCancelled()) {

            LOGGER.debug("Resetting timeout PONG");

            pingEvent.notifyObservers(this, PingEvent.PONG_CANCELLED);
            pongTimeoutFuture.cancel(false);
        }
    }


}
