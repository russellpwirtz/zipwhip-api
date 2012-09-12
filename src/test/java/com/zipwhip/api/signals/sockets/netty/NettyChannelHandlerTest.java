package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.api.signals.sockets.netty.pipeline.SignalsChannelHandler;
import junit.framework.Assert;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.handler.timeout.IdleState;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/23/12
 * Time: 4:15 PM
 */
public class NettyChannelHandlerTest {

    SignalsChannelHandler signalsChannelHandler;
    MockSignalConnectionDelegate delegate;
    MockChannel mockChannel;

    @Before
    public void doBefore() {
        delegate = new MockSignalConnectionDelegate();
        signalsChannelHandler = new SignalsChannelHandler(delegate);
        mockChannel = new MockChannel();
    }

    @Test
    public void testMessageReceivedNonCommand() throws Exception {

        MessageEvent event = new MessageEvent() {
            @Override
            public Object getMessage() {
                return new Object();
            }
            public SocketAddress getRemoteAddress() {return null;}
            public Channel getChannel() {return null;}
            public ChannelFuture getFuture() {return null;}
        };

        Assert.assertTrue(delegate.isConnected);
        signalsChannelHandler.messageReceived(null, event);

        Assert.assertEquals(0, delegate.pingEventCount);
        Assert.assertNull(delegate.receiveEventCommand);
        Assert.assertTrue(delegate.isConnected);
    }

    @Test
    public void testMessageReceivedPingPongCommand() throws Exception {

        MessageEvent event = new MessageEvent() {
            @Override
            public Object getMessage() {
                return PingPongCommand.getShortformInstance();
            }
            public SocketAddress getRemoteAddress() {return null;}
            public Channel getChannel() {return null;}
            public ChannelFuture getFuture() {return null;}
        };

        Assert.assertTrue(delegate.isConnected);
        signalsChannelHandler.messageReceived(null, event);

        Assert.assertEquals(0, delegate.pingEventCount);
        Assert.assertEquals(1, delegate.pongReceivedCount);
        Assert.assertNotNull(delegate.receiveEventCommand);
        Assert.assertTrue(delegate.receiveEventCommand instanceof PingPongCommand);
        Assert.assertTrue(delegate.isConnected);
    }

    @Test
    public void testMessageReceivedCommand() throws Exception {

        MessageEvent event = new MessageEvent() {
            public Object getMessage() {
                return new SignalCommand(new Signal());
            }
            public SocketAddress getRemoteAddress() {return null;}
            public Channel getChannel() {return null;}
            public ChannelFuture getFuture() {return null;}
        };

        Assert.assertTrue(delegate.isConnected);
        signalsChannelHandler.messageReceived(null, event);

        Assert.assertEquals(0, delegate.pongReceivedCount);
        Assert.assertNotNull(delegate.receiveEventCommand);
        Assert.assertTrue(delegate.receiveEventCommand instanceof SignalCommand);
        Assert.assertTrue(delegate.isConnected);
    }

    @Test
    public void testChannelIdle_READER_IDLE() throws Exception {

        IdleStateEvent event = new IdleStateEvent() {
            public IdleState getState() {return IdleState.READER_IDLE;}
            public long getLastActivityTimeMillis() {return 0;}
            public Channel getChannel() {return mockChannel;}
            public ChannelFuture getFuture() {return null;}
        };

        Assert.assertTrue(delegate.isConnected);
        signalsChannelHandler.channelIdle(null, event);
        Assert.assertTrue(delegate.isNetwork);
        Assert.assertFalse(delegate.isConnected);
    }

    @Test
    public void testChannelIdle_ALL_IDLE_whenConnected() throws Exception {

        IdleStateEvent event = new IdleStateEvent() {
            public IdleState getState() {return IdleState.ALL_IDLE;}
            public long getLastActivityTimeMillis() {return 0;}
            public Channel getChannel() {return mockChannel;}
            public ChannelFuture getFuture() {return null;}
        };

        Assert.assertTrue(delegate.isConnected);
        signalsChannelHandler.channelIdle(null, event);
        // SEND PING
        Assert.assertNotNull(delegate.sentCommand);
        Assert.assertTrue(delegate.sentCommand instanceof PingPongCommand);

        // NOTIFY PING
        Assert.assertEquals(1, delegate.pingEventCount);
        Assert.assertTrue(delegate.isConnected);
    }

    @Test
    public void testChannelIdle_ALL_IDLE_whenDisconnected() throws Exception {

        IdleStateEvent event = new IdleStateEvent() {
            public IdleState getState() {return IdleState.ALL_IDLE;}
            public long getLastActivityTimeMillis() {return 0;}
            public Channel getChannel() {return mockChannel;}
            public ChannelFuture getFuture() {return null;}
        };

//        delegate.isConnected = false;
//        Assert.assertFalse(delegate.isConnected());

        mockChannel.connected = false;
        Assert.assertFalse(mockChannel.isConnected());

        signalsChannelHandler.channelIdle(null, event);
        Assert.assertNull(delegate.sentCommand);
        Assert.assertEquals(0, delegate.pingEventCount);
        Assert.assertTrue(delegate.isNetwork);
        Assert.assertFalse(delegate.isConnected);
    }

    @Test
    public void testChannelClosed() throws Exception {

        ChannelStateEvent event = new ChannelStateEvent() {
            public Channel getChannel() {return null;}
            public ChannelFuture getFuture() {return null;}
            public ChannelState getState() {return null;}
            public Object getValue() {return null;}
        };

        Assert.assertTrue(delegate.isConnected);
        signalsChannelHandler.channelClosed(null, event);
        Assert.assertTrue(delegate.isNetwork);
        Assert.assertFalse(delegate.isConnected);
    }

    @Test
    public void testExceptionCaught() throws Exception {

        ExceptionEvent event = new ExceptionEvent() {
            public Throwable getCause() {return new Throwable("TEST EXCEPTION");}
            public Channel getChannel() {return null;}
            public ChannelFuture getFuture() {return null;}
            public String toString() {return "TEST EXCEPTION";}
        };

        Assert.assertTrue(delegate.isConnected);
        signalsChannelHandler.exceptionCaught(null, event);

        Assert.assertEquals(0, delegate.pingEventCount);
        Assert.assertNull(delegate.receiveEventCommand);
        Assert.assertEquals(1, delegate.exceptionCount);
        Assert.assertEquals("TEST EXCEPTION", delegate.exceptionString);
        Assert.assertFalse(delegate.isConnected);
    }

    private class MockSignalConnectionDelegate extends SignalConnectionDelegate {

        boolean isNetwork = false;
        boolean isConnected = true;
        int pongReceivedCount = 0;
        int pingEventCount = 0;
        int exceptionCount = 0;
        SerializingCommand sentCommand;
        Command receiveEventCommand;
        String exceptionString;

        public MockSignalConnectionDelegate(ConnectionHandle connectionHandle) {
            super(null, connectionHandle);
        }

        public MockSignalConnectionDelegate() {
            super(null, null);
        }

        @Override
        public void disconnectAsyncIfActive(boolean network) {
            isConnected = false;
            isNetwork = network;
        }

        @Override
        public synchronized void sendAsyncIfActive(SerializingCommand command) {
            sentCommand = command;
            try {
                signalsChannelHandler.writeRequested(null, new DownstreamMessageEvent(new MockChannel(), new DefaultChannelFuture(null, false), command, new InetSocketAddress(0)));
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized void receivePong(PingPongCommand command) {
            receiveEventCommand = command;
            pongReceivedCount++;
        }

        @Override
        public synchronized void notifyReceiveEvent(Command command) {
            receiveEventCommand = command;
        }

        @Override
        public synchronized void notifyExceptionAndDisconnect(String result) {
            exceptionCount++;
            exceptionString = result;
            disconnectAsyncIfActive(true);
        }

        @Override
        public synchronized void notifyPingEvent(PingEvent event) {
            pingEventCount++;
        }
    }

    private class MockChannel extends AbstractChannel {

        public boolean connected = true;

        protected MockChannel() {
            super(null,null , new DefaultChannelPipeline(), new MockChannelSink());
        }

        @Override
        public ChannelConfig getConfig() {
            return null;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public boolean isWritable() {
            return connected;
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

    }

    private class MockChannelSink  implements ChannelSink {
        public void eventSunk(ChannelPipeline pipeline, ChannelEvent e) throws Exception {}
        public void exceptionCaught(ChannelPipeline pipeline, ChannelEvent e, ChannelPipelineException cause) throws Exception {}
        public ChannelFuture execute(ChannelPipeline pipeline, Runnable task) {return null;}
    }

}
