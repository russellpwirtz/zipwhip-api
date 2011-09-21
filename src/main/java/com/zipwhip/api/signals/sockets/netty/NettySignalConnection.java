package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.DefaultReconnectStrategy;
import com.zipwhip.api.signals.ReconnectStrategy;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.*;

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 8/2/11 Time: 11:49 AM
 * <p/>
 * Connects to the SignalServer via Netty
 */
public class NettySignalConnection extends DestroyableBase implements SignalConnection, ChannelPipelineFactory {

    private static final Logger LOGGER = Logger.getLogger(NettySignalConnection.class);

    public static final int CONNECTION_TIMEOUT_SECONDS = 45;

    private static final int MAX_FRAME_SIZE = 65535;

    private static final int DEFAULT_PING_TIMEOUT = 1000 * 300; // when to ping, inactive seconds
    private static final int DEFAULT_PONG_TIMEOUT = 1000 * 30; // when to disconnect if a ping was not ponged by this time

    private String host = "signals.zipwhip.com";
    private int port = 3000;

    private int pingTimeout = DEFAULT_PING_TIMEOUT;
    private int pongTimeout = DEFAULT_PONG_TIMEOUT;

    private ExecutorService executor;

    private ScheduledFuture<?> pingTimeoutFuture;
    private ScheduledFuture<?> pongTimeoutFuture;
    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

    private ObservableHelper<Void> pingEvent = new ObservableHelper<Void>();
    private ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
    private ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();
    private ObservableHelper<Boolean> disconnectEvent = new ObservableHelper<Boolean>();

    private ReconnectStrategy reconnectStrategy;

    private Channel channel;
    private ChannelFactory channelFactory;

    private boolean networkDisconnect;

    /**
     * Create a new {@code NettySignalConnection} with a default {@code ReconnectStrategy}.
     */
    public NettySignalConnection() {
        this(new DefaultReconnectStrategy());
    }

    /**
     * Create a new {@code NettySignalConnection}.
     *
     * @param reconnectStrategy The reconnect strategy to use in the case of socket disconnects.
     */
    public NettySignalConnection(ReconnectStrategy reconnectStrategy) {

        this.link(pingEvent);
        this.link(receiveEvent);
        this.link(connectEvent);
        this.link(disconnectEvent);
        this.link(reconnectStrategy);

        this.reconnectStrategy = reconnectStrategy;
        this.reconnectStrategy.setSignalConnection(this);
    }

    @Override
    public synchronized Future<Boolean> connect() throws Exception {

        channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        channel = channelFactory.newChannel(getPipeline());

        final ChannelFuture channelFuture = channel.connect(new InetSocketAddress(host, port));

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {

                channelFuture.await(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                boolean socketConnected = !(channelFuture.isCancelled() || !channelFuture.isSuccess()) && channelFuture.getChannel().isConnected();

                networkDisconnect = socketConnected;

                connectEvent.notifyObservers(this, socketConnected);

                return socketConnected;
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
        return disconnect(false);
    }

    @Override
    public Future<Void> disconnect(final boolean network) {

        FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                networkDisconnect = network;

                if (channel != null) {
                    ChannelFuture disconnectFuture = channel.disconnect().await();
                    LOGGER.debug("Disconnecting success was " + disconnectFuture.isSuccess());
                }

                if (channelFactory != null) {
                    channelFactory.releaseExternalResources();
                }

                executor.shutdownNow();
                executor = null;

                if (pingTimeoutFuture != null) {
                    pingTimeoutFuture.cancel(true);
                }

                if (pongTimeoutFuture != null) {
                    pongTimeoutFuture.cancel(true);
                }

                return null;
            }
        });

        executor.execute(task);

        return task;
    }

    @Override
    public void send(SerializingCommand command) {
        // send this over the wire.
        channel.write(command);
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isConnected();
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
    public void onPing(Observer<Void> observer) {
        pingEvent.addObserver(observer);
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
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
            this.reconnectStrategy.destroy();
        }

        this.reconnectStrategy = reconnectStrategy;
        this.reconnectStrategy.setSignalConnection(this);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {

        return Channels.pipeline(
                // Second arg must be set to false. This tells Netty not to strip the frame delimiter so we can recognise PONGs upstream.
                new DelimiterBasedFrameDecoder(MAX_FRAME_SIZE, false, copiedBuffer(StringToChannelBuffer.CRLF, Charset.defaultCharset())),
                new StringToChannelBuffer(),
                new StringDecoder(),
                new MessageDecoder(),
                new SignalCommandEncoder(),
                new SimpleChannelHandler() {

                    /**
                     * The entry point for signal traffic
                     *
                     * @param ctx ChannelHandlerContext
                     * @param e MessageEvent
                     * @throws Exception
                     */
                    @Override
                    public void messageReceived(final ChannelHandlerContext ctx, MessageEvent e) throws Exception {

                        Object msg = e.getMessage();

                        if (!(msg instanceof Command)) {

                            LOGGER.warn("Received a message that was not a command!");

                            return;

                        } else if (msg instanceof PingPongCommand) {

                            // We received a PONG, cancel the PONG timeout.
                            receivePong((PingPongCommand) msg);

                            return;

                        } else {

                            // We have activity on the wire, reschedule the next PING
                            schedulePing();
                        }

                        Command command = (Command) msg;

                        receiveEvent.notifyObservers(this, command);
                    }

                    @Override
                    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                        LOGGER.debug("channelClosed");
                        disconnectEvent.notifyObservers(this, networkDisconnect);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
                        LOGGER.error(e);
                        // TODO queue and report to the server
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {

        if (isConnected()) {
            disconnect();
        }

        if(pingTimeoutFuture != null) {
            pingTimeoutFuture.cancel(true);
        }

        if(pongTimeoutFuture != null) {
            pongTimeoutFuture.cancel(true);
        }

        if(scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
        }
    }

    private void schedulePing() {

        if (pingTimeoutFuture != null && !pingTimeoutFuture.isCancelled()) {

            if (pingTimeoutFuture != null) {

                LOGGER.debug("Resetting scheduled PING");

                pingTimeoutFuture.cancel(false);
            }
        }

        LOGGER.debug("Scheduling a PING");

        pingTimeoutFuture = scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {

                LOGGER.debug("Sending a PING");

                send(PingPongCommand.getShortformInstance());

                pingEvent.notifyObservers(this, null);

                pongTimeoutFuture = scheduledExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {

                        LOGGER.warn("PONG timeout, disconnecting...");

                        disconnect(true);
                    }
                }, pongTimeout, TimeUnit.MILLISECONDS);

            }
        }, pingTimeout, TimeUnit.MILLISECONDS);
    }

    private void receivePong(PingPongCommand command) {

        if (command.isRequest()) {

            LOGGER.debug("Received a REVERSE PING");

            PingPongCommand reversePong = PingPongCommand.getNewLongformInstance();
            reversePong.setTimestamp(command.getTimestamp());
            reversePong.setToken(command.getToken());

            LOGGER.debug("Sending a REVERSE PONG");
            send(reversePong);

        } else {

            LOGGER.debug("Received a PONG");
        }

        if (pongTimeoutFuture != null && !pongTimeoutFuture.isCancelled()) {

            LOGGER.debug("Resetting timeout PONG");

            pongTimeoutFuture.cancel(false);
        }

        schedulePing();
    }

}
