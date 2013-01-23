package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.ConnectionState;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: me
 * Date: 1/21/13
 * Time: 12:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketSignalConnection implements SignalConnection {


    private void init() {

        WebsocketCli

        final ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("handler", new WebSocketPacketHandler());
        pipeline.addLast("handler", new WebSocketServerHandler());
        return pipeline;
    }


    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws IOException {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public ConnectionHandle getConnectionHandle() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ConnectionState getConnectionState() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ObservableFuture<ConnectionHandle> connect() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ObservableFuture<ConnectionHandle> reconnect() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ObservableFuture<ConnectionHandle> disconnect() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ObservableFuture<ConnectionHandle> disconnect(boolean network) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ObservableFuture<Boolean> ping() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ObservableFuture<Boolean> send(SerializingCommand command) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Observable<ConnectionHandle> getConnectEvent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Observable<ConnectionHandle> getDisconnectEvent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Observable<Command> getCommandReceivedEvent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Observable<PingEvent> getPingEventReceivedEvent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Observable<String> getExceptionEvent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAddress(SocketAddress address) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SocketAddress getAddress() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ReconnectStrategy getReconnectStrategy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setReconnectStrategy(ReconnectStrategy strategy) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getConnectTimeoutSeconds() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isDestroyed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
