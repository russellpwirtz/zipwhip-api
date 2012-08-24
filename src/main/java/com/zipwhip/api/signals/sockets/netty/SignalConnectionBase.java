package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.CascadingDestroyableBase;
import com.zipwhip.lifecycle.Destroyable;
import com.zipwhip.util.SocketAddressUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author jed
 */
public abstract class SignalConnectionBase extends CascadingDestroyableBase implements SignalConnection {

    protected static final Logger LOGGER = Logger.getLogger(SignalConnectionBase.class);

    private final Object WRAPPER_BEING_TOUCHED_LOCK = new Object();

    private static int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 45;

    private String host = "74.209.177.242";
    private int port = 80;
    private int connectionTimeoutSeconds = DEFAULT_CONNECTION_TIMEOUT_SECONDS;

    protected final ObservableHelper<PingEvent> pingEvent = new ObservableHelper<PingEvent>();
    protected final ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
    protected final ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
    protected final ObservableHelper<String> exceptionEvent = new ObservableHelper<String>();
    protected final ObservableHelper<Boolean> disconnectEvent = new ObservableHelper<Boolean>();

    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    protected ReconnectStrategy reconnectStrategy;

    protected ChannelWrapper wrapper;
    protected final ChannelWrapperFactory channelWrapperFactory;

    private final ChannelFactory channelFactory = new OioClientSocketChannelFactory(Executors.newSingleThreadExecutor());

    /**
     * Protected constructor for subclasses.
     *
     * @param channelPipelineFactory The factory instantiate the ChannelPipeline
     */
    protected SignalConnectionBase(ChannelPipelineFactory channelPipelineFactory) {

        if (channelPipelineFactory == null) {
            channelPipelineFactory = new RawSocketIoChannelPipelineFactory();
        }

        if (reconnectStrategy == null) {
            this.setReconnectStrategy(reconnectStrategy);
        }

        this.channelWrapperFactory = new ChannelWrapperFactory(channelPipelineFactory, channelFactory, this);

        this.link(channelWrapperFactory);
        this.link(pingEvent);
        this.link(receiveEvent);
        this.link(connectEvent);
        this.link(exceptionEvent);
        this.link(disconnectEvent);

        // start the reconnectStrategy whenever we do a connect
        this.connectEvent.addObserver(new StartReconnectStrategyObserver(this));

        // stop the reconnectStrategy whenever we disconnect manually.
        this.disconnectEvent.addObserver(new StopReconnectStrategyObserver(this));
    }

    @Override
    public synchronized Future<Boolean> connect() throws Exception {

        // Enforce a single connection
        validateNotConnected();
        assert this.wrapper == null;

        // prevent any side requests from any other threads from getting in.
        cancelReconnectStrategy();

        // immediately/synchronously create the wrapper.
        final ChannelWrapper channelWrapper = this.wrapper = channelWrapperFactory.create();

        return FutureUtil.execute(this.executor, new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {

                // maybe unneeded sanity check.
                validateNotConnected();

                assert channelWrapper == wrapper;

                try {
                    final InetSocketAddress address = SocketAddressUtil.getSingle(host, port);

                    LOGGER.debug("Connecting to " + address);

                    // synchronous connection to endpoint, will crash if not successful.
                    channelWrapper.connect(address);
                } catch (InterruptedException e) {
                    // timeout?
                    channelWrapper.destroy();
                    wrapper = null;

                    connectEvent.notifyObservers(this, isConnected());
                    disconnectEvent.notifyObservers(this, Boolean.TRUE);
                    return Boolean.FALSE;
                }

                // the rule is that we destroy the wrapper if it's not connected.
                if (!isConnected()) {
                    synchronized (WRAPPER_BEING_TOUCHED_LOCK) {
                        channelWrapper.destroy();
                        wrapper = null;
                    }
                }

                // restore the strategy so it can listen to these events.
                startReconnectStrategy();

                connectEvent.notifyObservers(this, isConnected());

                if (!isConnected()) {
                    disconnectEvent.notifyObservers(this, Boolean.TRUE);
                }

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
        cancelReconnectStrategy();

        return FutureUtil.execute(executor, new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                // sanity check the state to make sure it hasn't changed by the time we ran.
                validateConnected();

                synchronized (WRAPPER_BEING_TOUCHED_LOCK) {

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

                disconnectEvent.notifyObservers(this, network);

                return null;
            }

        });
    }

    @Override
    public void keepalive() {
        LOGGER.debug("Keepalive requested");
        send(PingPongCommand.getShortformInstance());
    }

    @Override
    public synchronized Future<Boolean> send(final SerializingCommand command) throws IllegalStateException {

        validateConnected();

        if(!wrapper.channel.isWritable()) {
            throw new IllegalStateException("Channel is not writable.");
        }

        // send this over the wire.
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
    public final int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectTimeoutSeconds;
    }

    @Override
    public int getConnectTimeoutSeconds() {
        return connectionTimeoutSeconds;
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

    @Override
    protected void onDestroy() {
        if (isConnected()) {
            disconnect();
        }

        if (channelFactory instanceof Destroyable) {
            ((Destroyable) channelFactory).destroy();
        } else {
            channelFactory.releaseExternalResources();
        }
    }

}
