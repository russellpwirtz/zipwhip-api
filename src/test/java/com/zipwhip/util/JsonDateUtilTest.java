package com.zipwhip.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.zipwhip.api.Address;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/12/11
 * Time: 10:41 AM
 */
public class JsonDateUtilTest {

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testGetDateFromObject() throws Exception {

		Date d = JsonDateUtil.getDate(null);
		Assert.assertNull(d);

		d = JsonDateUtil.getDate(new Address());
		Assert.assertNull(d);
	}

	@Test
	public void testGetDateFromString() throws Exception {

		Date d = JsonDateUtil.getDate(StringUtil.EMPTY_STRING);
		Assert.assertNull(d);

		String dateString = "2011-09-12T11:01:02";
		d = JsonDateUtil.getDate(dateString);
		Assert.assertNotNull(d);

		Calendar c = new GregorianCalendar();
		c.setTime(d);

		// Yes, MONTH is 0 based as opposed to all the other fields, lame
		Assert.assertEquals(c.get(Calendar.MONTH), 8);
		Assert.assertEquals(c.get(Calendar.YEAR), 2011);
		Assert.assertEquals(c.get(Calendar.DAY_OF_MONTH), 12);
		Assert.assertEquals(c.get(Calendar.HOUR_OF_DAY), 11);
		Assert.assertEquals(c.get(Calendar.MINUTE), 1);
		Assert.assertEquals(c.get(Calendar.SECOND), 2);
	}

	@Test
	public void testGetDateFromTimestamp() throws Exception {

		Date d1 = new Date();

		Date d2 = JsonDateUtil.getDate(d1.getTime());

		Assert.assertNotNull(d2);
		Assert.assertEquals(d1, d2);
	}

	@Test
	public void testGetANormalDate() {

        String dateString = "Jan 26, 2012 3:45:36 PM";
        Date d = JsonDateUtil.getDate(dateString);
        Assert.assertNotNull(d);

        Calendar c = new GregorianCalendar();
        c.setTime(d);

        // Yes, MONTH is 0 based as opposed to all the other fields, lame
        Assert.assertEquals(c.get(Calendar.MONTH), 0);
        Assert.assertEquals(c.get(Calendar.YEAR), 2012);
        Assert.assertEquals(c.get(Calendar.DAY_OF_MONTH), 26);
        Assert.assertEquals(c.get(Calendar.HOUR_OF_DAY), 15);
        Assert.assertEquals(c.get(Calendar.MINUTE), 45);
        Assert.assertEquals(c.get(Calendar.SECOND), 36);
	}

}
