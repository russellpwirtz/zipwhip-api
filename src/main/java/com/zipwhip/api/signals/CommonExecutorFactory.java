package com.zipwhip.api.signals;

import com.zipwhip.api.signals.sockets.CommonExecutorTypes;
import com.zipwhip.concurrent.ConfiguredFactory;

import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/12/12
 * Time: 3:01 PM
 *
 *
 */
public interface CommonExecutorFactory {

    ExecutorService create(CommonExecutorTypes type, String name);

}
