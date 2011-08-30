package com.zipwhip.api.request;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 1:54 PM
 */
public class RequestBuilderTest {

    RequestBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new RequestBuilder();
    }

    @Test
    public void testBuild() throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("1","2");
        params.put("3","4");

        builder.params(params);
        String query1 = builder.build();

        Assert.assertEquals("?3=4&1=2", query1);

        builder = new RequestBuilder();
        builder.param("3","4");
        builder.param("1","2");
        String query2 = builder.build();

        Assert.assertEquals(query1, query2);
    }
}
