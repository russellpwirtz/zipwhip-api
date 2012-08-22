package com.zipwhip.api.signals.sockets.netty;

import org.jboss.netty.channel.ChannelPipeline;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 8/21/12
 * Time: 1:56 PM
 *
 * It's a pipelineFactory (see netty) that is aware of its delegate
 */
public interface SignalChannelPipelineFactory {

    ChannelPipeline create(SignalConnectionDelegate delegate);

}
