package com.zipwhip.api.signals.sockets.netty;

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

    public static final int CONNECTION_TIMEOUT_SECONDS = 45;

    private static final int MAX_FRAME_SIZE = 65535;
    private static final int PING_TIMEOUT = 1000 * 300; // when to ping, inactive seconds
    private static final int PONG_TIMEOUT = 1000 * 30; // when to disconnect if a ping was not ponged by this time

    private static final Logger logger = Logger.getLogger(NettySignalConnection.class);

    private String host = "signals.zipwhip.com";
    private int port = 80;

    private ExecutorService executor;

    private ScheduledFuture<?> pingTimeoutFuture;
    private ScheduledFuture<?> pongTimeoutFuture;
    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

    private ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>();
    private ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>();

    private Channel channel;
    private ChannelFactory channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

    public NettySignalConnection() {
        this.link(receiveEvent);
        this.link(connectEvent);
    }

    @Override
    public synchronized Future<Boolean> connect() throws Exception {

        channel = channelFactory.newChannel(getPipeline());
        final ChannelFuture channelFuture = channel.connect(new InetSocketAddress(host, port));

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {

                channelFuture.await(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                boolean socketConnected = !(channelFuture.isCancelled() || !channelFuture.isSuccess()) && channelFuture.getChannel().isConnected();

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

        FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                if (channel != null) {
                    channel.disconnect().await();
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

                connectEvent.notifyObservers(this, false);

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
    public void onConnectionStateChanged(Observer<Boolean> observer) {
        connectEvent.addObserver(observer);
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

                            logger.warn("Received a message that was not a command!");

                            return;

                        } else if (msg instanceof PingPongCommand) {

                            // We received a PONG, cancel the PONG timeout.
                            receivePong();

                            return;

                        } else {

                            // We have activity on the wire, reschedule the next PING
                            schedulePing();
                        }

                        Command command = (Command) msg;

                        receiveEvent.notifyObservers(this, command);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
                        // TODO queue and report to the server
                        logger.error(e);
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {

        if (isConnected()) {
            disconnect();
        }

        pingTimeoutFuture.cancel(true);
        pongTimeoutFuture.cancel(true);
        scheduledExecutor.shutdownNow();
    }

    private void schedulePing() {

        if (pingTimeoutFuture != null && !pingTimeoutFuture.isCancelled()) {

            if (pingTimeoutFuture != null) {

                logger.debug("Resetting scheduled PING");

                pingTimeoutFuture.cancel(false);
            }
        }

        logger.debug("Scheduling a PING");

        pingTimeoutFuture = scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {

                logger.debug("Sending a PING");

                send(PingPongCommand.getInstance());

                pongTimeoutFuture = scheduledExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {

                        logger.warn("PONG timeout, disconnecting...");

                        disconnect();
                    }
                }, PONG_TIMEOUT, TimeUnit.MILLISECONDS);

            }
        }, PING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private void receivePong() {

        logger.debug("Received a PONG");

        if (pongTimeoutFuture != null && !pongTimeoutFuture.isCancelled()) {

            logger.debug("Resetting timeout PONG");

            pongTimeoutFuture.cancel(false);
        }

        schedulePing();
    }

}
