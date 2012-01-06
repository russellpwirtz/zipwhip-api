package com.zipwhip.signals.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Parker
 * Date: Dec 13, 2010
 * Time: 8:38:10 PM
 *
 * A utility class that provides the encoding json wrapping.
 *
 */
public class EncoderUtil<T> {

	private static final String CLASS = "class";

	public static Map<String, Object> serialize(Object object) {
		return serialize(object.getClass());
	}

	public static <T> Map<String, Object> serialize(Class<T> object)
	{
		Map<String, Object> map = new HashMap<String, Object>();

		map.put(CLASS, object.getSimpleName());

		return map;
	}

}
