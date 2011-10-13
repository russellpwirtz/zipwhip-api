package com.zipwhip.vendor;

import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.concurrent.NetworkFuture;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 10:39 AM
 */
public class DefaultAsyncVendorClientTest {

    String apiKey = "18adc2";
    String secret = "249fasdasdff5cc-5aasdfdf2-4707-9f5e-da1893b92e851dfb4ee0-4156-469b-8ee3-d50912";

    AsyncVendorClient client;

//    @Before
//    public void setUp() throws Exception {
//
//        BasicConfigurator.configure();
//
//        client = AsyncVendorClientFactory.createViaApiKey(apiKey, secret);
//        client.getConnection().setHost("http://10.168.1.92:8080");
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        BasicConfigurator.resetConfiguration();
//    }
//
//    @Test
//    public void testEnrollUser() throws Exception {
//
//        NetworkFuture<EnrollmentResult> result = client.enrollUser("5555555557");
//        Assert.assertNotNull(result);
//
//        result.await();
//        Assert.assertTrue(result.isSuccess());
//    }
//
//    @Test
//    public void testDeactivateUser() throws Exception {
//
//    }
//
//    @Test
//    public void testUserExists() throws Exception {
//
//    }
//
//    @Test
//    public void testSuggestCarbon() throws Exception {
//
//    }
//
//    @Test
//    public void testSendMessage() throws Exception {
//
//    }
//
//    @Test
//    public void testListMessages() throws Exception {
//
//    }
//
//    @Test
//    public void testSaveUser() throws Exception {
//
//    }
//
//    @Test
//    public void testReadMessages() throws Exception {
//
//    }
//
//    @Test
//    public void testDeleteMessages() throws Exception {
//
//    }
//
//    @Test
//    public void testReadConversations() throws Exception {
//
//    }
//
//    @Test
//    public void testDeleteConversations() throws Exception {
//
//    }
//
//    @Test
//    public void testListConversations() throws Exception {
//
//    }
//
//    @Test
//    public void testSaveContact() throws Exception {
//
//    }
//
//    @Test
//    public void testDeleteContact() throws Exception {
//
//    }
//
//    @Test
//    public void testListContacts() throws Exception {
//        NetworkFuture<List<Contact>> f = client.listContacts("5555555555");
//        Assert.assertNotNull(f);
//        f.await();
//    }

}
