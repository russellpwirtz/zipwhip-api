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
    private static final int WRITE_TIMEOUT_SECONDS = 30;

    final ChannelWrapper wrapper;
    final Object message;

    public WriteOnChannelSafelyCallable(ChannelWrapper wrapper, Object message) {
        this.wrapper = wrapper;
        this.message = message;
    }

    @Override
    public Boolean call() throws Exception {
        synchronized (wrapper) {
            if (wrapper.isDestroyed()) {
                // it seems that we aren't connected?
                return Boolean.FALSE;
            }
        }

        ChannelFuture future;

        // this channel.write is NOT async like we thought. It's actually
        // an OIO non sync.
//        synchronized (wrapper) {
            // dont let anyone destroy the wrapper (and channel) during access.
            future = wrapper.channel.write(message);
//        }

        boolean finished = future.await(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            // TODO: this might cause the client to RE-TRANSMIT a request that the server actually did receive.
            // what's the right action here??
            future.cancel();
            return Boolean.FALSE;
        }

        if (future.isCancelled()) {
            LOGGER.warn("The future was cancelled.");
        }

        // TODO: is this the right?
        if (future.getCause() != null) {
            throw new Exception(future.getCause());
        }

        return future.isSuccess();
    }
}
