package com.zipwhip.api.signals.sockets.netty;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 3:25 PM
 * <p/>
 * Write on a channel and report back the success/failure
 */
public class WriteOnChannelSafelyCallable implements Callable<Boolean> {

    private static final Logger LOGGER = Logger.getLogger(WriteOnChannelSafelyCallable.class);

    ChannelWrapper wrapper;
    Object message;

    public WriteOnChannelSafelyCallable(ChannelWrapper wrapper, Object message) {
        this.wrapper = wrapper;
        this.message = message;
    }

    @Override
    public Boolean call() throws InterruptedException {
        if (!wrapper.isConnected() || wrapper.isDestroyed()) {
            // it seems that we aren't connected?
            return Boolean.FALSE;
        }

        ChannelFuture future = wrapper.channel.write(message);

        boolean finished = future.await(SignalConnectionBase.CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            // TODO: this might cause the client to RE-TRANSMIT a request that the server actually did receive.
            // what's the right action here??
            future.cancel();
            return Boolean.FALSE;
        }

        if (future.isCancelled()) {
            LOGGER.warn("The future was cancelled.");
        }

        return future.isSuccess();
    }
}
