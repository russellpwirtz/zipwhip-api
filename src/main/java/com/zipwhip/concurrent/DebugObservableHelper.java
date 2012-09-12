package com.zipwhip.concurrent;

import com.zipwhip.events.ObservableHelper;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: Michael
 * Date: 9/11/12
 * Time: 2:36 PM
 *
 */
public class DebugObservableHelper<T> extends ObservableHelperAdapter<T> {

    private static final Logger LOGGER = Logger.getLogger(DebugObservableHelper.class);

    public DebugObservableHelper(ObservableHelper<T> observableHelper) {
        super(observableHelper);
    }

    @Override
    public void notifyObservers(Object sender, T result) {
        LOGGER.debug(String.format("%s: Notifying event [%s, %s]", this, sender, result));
        super.notifyObservers(sender, result);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
