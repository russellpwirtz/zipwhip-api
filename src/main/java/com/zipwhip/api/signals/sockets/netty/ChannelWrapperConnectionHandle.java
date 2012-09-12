package com.zipwhip.api.signals.sockets.netty;

import com.zipwhip.api.signals.Writable;
import com.zipwhip.api.signals.sockets.ConnectionHandle;
import com.zipwhip.concurrent.ObservableFuture;

/**
 * Created with IntelliJ IDEA.
 * User: msmyers
 * Date: 9/4/12
 * Time: 2:20 PM
 * <p/>
 * This class allows callers to have "correct ensureAbleTo" to the underlying connection. They might need to disconnect
 * THE SPECIFIC connection that they created. If another connection has taken its place then they want to just noop.
 */
public class ChannelWrapperConnectionHandle extends SignalConnectionBaseConnectionHandleBase implements ConnectionHandle, Writable {

    public ChannelWrapper channelWrapper;
    // we use pause to prevent any concurrent ensureAbleTo. Is this needed?

    public ChannelWrapperConnectionHandle(long id, SignalConnectionBase signalConnectionBase, ChannelWrapper channelWrapper) {
        super(id, signalConnectionBase);

        this.channelWrapper = channelWrapper;
    }

    @Override
    public ObservableFuture<Boolean> write(Object object) {
        return channelWrapper.write(object);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        channelWrapper = null;
    }

    @Override
    public String toString() {
        if (channelWrapper == null){
            return "[ChannelWrapperConnectionHandle: null]";
        } else {
            return String.format("[ChannelWrapperConnectionHandle: %s]", channelWrapper.channel);
        }
    }
}
