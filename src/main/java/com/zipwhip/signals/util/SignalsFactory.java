/**
 * 
 */
package com.zipwhip.signals.util;

import java.util.Map;

/**
 * @author jdinsel
 *
 */
public interface SignalsFactory<T> {
	public T create(Map<String, Object> properties);
}
