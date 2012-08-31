package com.zipwhip.api.signals.sockets.netty.pipeline.handler;

import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.signals.server.protocol.SocketIoProtocol;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * The whole purpose of this class is to generate a pong timeout by intercepting a ping and dropping it.
 */
public class TestRawSocketIoChannelPipelineFactory extends DestroyableBase implements ChannelPipelineFactory {

    public static final int DEFAULT_FRAME_SIZE = 8192;

    private final IdleStateHandler idleStateHandler;
    private final StringDecoder stringDecoder = new StringDecoder();
    private final StringEncoder stringEncoder = new StringEncoder();
    private final SocketIoCommandDecoder socketIoCommandDecoder = new SocketIoCommandDecoder();
    private final Timer idleChannelTimer = new HashedWheelTimer();

    public TestRawSocketIoChannelPipelineFactory(int pingIntervalSeconds, int pongTimeoutSeconds) {
        idleStateHandler = new SocketIdleStateHandler(idleChannelTimer, pingIntervalSeconds, pongTimeoutSeconds);
    }

    /*
    * (non-Javadoc)
    *
    * @see com.zipwhip.api.signals.sockets.netty.SignalConnectionBase#getPipeline()
    */
    @Override
    public ChannelPipeline getPipeline() {
        return Channels.pipeline(
                new DelimiterBasedFrameDecoder(DEFAULT_FRAME_SIZE, Delimiters.lineDelimiter()),
                stringDecoder,
                socketIoCommandDecoder,
                stringEncoder,
                new DoNotWritePingsSocketIoCommandEncoder(),
                idleStateHandler
        );
    }

    @Override
    protected void onDestroy() {
        idleChannelTimer.stop();
    }

    public class DoNotWritePingsSocketIoCommandEncoder extends OneToOneEncoder implements ChannelHandler {

        @Override
        protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {

            System.out.println("encoding...");

            String message = null;

            if (msg instanceof ConnectCommand) {

                System.out.println("encoding ConnectCommand");
                ConnectCommand command = (ConnectCommand) msg;

                message = SocketIoProtocol.connectMessageResponse(command.serialize(), command.getClientId());

            } else if (msg instanceof PingPongCommand) {

                System.out.println("Dropping a ping...");

            } else if (msg instanceof SerializingCommand) {

                System.out.println("encoding SerializingCommand");
                SerializingCommand command = (SerializingCommand) msg;

                message = SocketIoProtocol.jsonMessageResponse(0, command.serialize());
            }
            return message;
        }

    }

}