package com.zipwhip.signals.app;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: Dec 13, 2010
 * Time: 2:08:45 AM
 *
 * Allows you to load/save an object to/from a map.
 * 
 */
public interface MapSerializable {

	public Map<String, Object> save();

	public void load(Map<String, Object> properties);

}
