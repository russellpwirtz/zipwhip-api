package com.zipwhip.signals.app;

import com.zipwhip.signals.message.Action;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 11, 2010
 * Time: 4:34:38 PM
 * 
 * A request.
 * 
 * @deprecated Use SignalCommand in the com.zipwhip.api package instead
 */
@Deprecated
public interface Command  {

	public Action getAction();

}
