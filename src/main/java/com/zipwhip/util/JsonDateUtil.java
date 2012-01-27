package com.zipwhip.util;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class JsonDateUtil {

	private static final Format GRAILS_DATE_FORMATTER = new ISO8601DateTimeFormat();
	private static final Logger LOGGER = Logger.getLogger(JsonDateUtil.class);

	private static final SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy h:mm:ss aaa");

	/**
	 * Utility class --> private constructor
	 */
	private JsonDateUtil() {
	}

	/**
	 * Takes an object and returns a java.util.Date.
	 * Supported objects are String (format: yyyy-MM-ddTHH:mm:ssZ)
	 * and Long (date in milli seconds)
	 * The time zone for the input date string is considered to be in GMT by
	 * default (+0000)
	 * Returns null in case of an exception
	 *
	 * @param date - Standard Grails Formatted String Date
	 * @return Date - java.util.Date object
	 */
	public static Date getDate(Object date) {

		//Check for null
		if (date == null) {
			return null;
		}

		//Call the right parser method depending on the input datatype
		if (date instanceof String) {
			return getDate(date.toString());
		} else if (date instanceof Long) {
			long longDate = (Long) date;
			return getDate(longDate);
		} else if (date instanceof Date) { // we need to handle the backwards compatible case
			return (Date) date;
		}

		LOGGER.warn("Unsupported datatype! " + date.getClass().getName());
		return null;
	}

	/**
	 * Takes a string date in the grails default format (yyyy-MM-ddTHH:mm:ssZ)
	 * and returns a java.util.Date
	 * The time zone for the input date string is considered to be in GMT by
	 * default (+0000)
	 * Returns null in case of an exception
	 *
	 * @param stringDate - Standard Grails Formatted String Date
	 * @return Date - java.util.Date
	 */
	public static Date getDate(String stringDate) {

		//Check for null, string length
		if ((stringDate == null) || (stringDate.length() <= 0)) {
			return null;
		}

		try {
			Date parse = formatter.parse(stringDate);
			if (parse != null) {
				return parse;
			}
		} catch (ParseException e) {
			// fall back to the Grails thing
		}

		// Parse and return date
		try {
			synchronized (GRAILS_DATE_FORMATTER) {
				return (Date) GRAILS_DATE_FORMATTER.parseObject(stringDate.toUpperCase());
			}
		} catch (ParseException pex) {
			LOGGER.error(pex);
			return null;
		}
	}

	/**
	 * Takes a long date timestamp and returns a java.util.Date
	 *
	 * @param milliDate - date in milli seconds
	 * @return Date - java.util.Date
	 */
	public static Date getDate(long milliDate) {
		return new Date(milliDate);
	}

}
