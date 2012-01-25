package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Conversation;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.concurrent.DefaultObservableFuture;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.lifecycle.DestroyableBase;
import com.zipwhip.util.SignTool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 10/12/11
 * Time: 10:39 AM
 */
public class DefaultAsyncVendorClientTest {

    AsyncVendorClient client;

    String apiKey = "a1b2c3";
    String secret = "5241fvv354-v5b73nm6j5w4nb64ff-423c53-v345bn6nbq5v";
    String deviceAddress = "device:/2069998888/0";
    String contactMobileNumber = "2063758020";

    public final static String VOID_RESULT = "{\"response\":null,\"sessions\":null,\"success\":true}";
    public final static String BOOLEAN_TRUE_RESULT = "{\"response\":true,\"sessions\":null,\"success\":true}";
    public final static String ENROLLMENT_RESULT = "{\"response\":{\"carbonEnabled\":true,\"carbonInstalled\":true,\"deviceNumber\":999},\"sessions\":null,\"success\":true}";
    public final static String CONTACT_LIST_RESULT = "{\"total\":1,\"response\":[{\"birthday\":null,\"state\":\"\",\"version\":4,\"dtoParentId\":270315,\"city\":\"\",\"id\":408050,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"20000002\",\"phoneId\":0,\"carrier\":\"Tmo\",\"firstName\":\"\",\"deviceId\":270315,\"lastName\":\"\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-10-14T14:59:51-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"20000102\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-10-14T14:58:54-07:00\",\"mobileNumber\":\"2533654478\",\"notes\":\"\",\"channel\":\"2\"}],\"sessions\":null,\"page\":1,\"pages\":1,\"success\":true}";
    public final static String CONTACT_SAVE_RESULT = "{\"response\":{\"birthday\":null,\"state\":\"\",\"version\":1,\"dtoParentId\":270315,\"city\":\"\",\"id\":408077,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"\",\"phoneId\":0,\"carrier\":\"\",\"firstName\":\"Jon\",\"deviceId\":270315,\"lastName\":\"Dow\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-10-17T13:19:48-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"device:/5555555555/0\",\"dateCreated\":\"2011-10-17T13:19:48-07:00\",\"mobileNumber\":\"5555555555\",\"notes\":\"\",\"channel\":\"\"},\"sessions\":null,\"success\":true}";
    public final static String CONVERSATION_LIST_RESULT = "{\"total\":1,\"response\":[{\"lastContactFirstName\":\"\",\"lastContactLastName\":\"\",\"lastContactDeviceId\":0,\"unreadCount\":0,\"bcc\":\"\",\"lastUpdated\":\"2011-10-14T14:59:51-07:00\",\"class\":\"com.zipwhip.website.data.dto.Conversation\",\"deviceAddress\":\"device:/2063758020/0\",\"lastNonDeletedMessageDate\":\"2011-10-14T14:59:51-07:00\",\"deleted\":false,\"lastContactId\":408050,\"version\":2,\"lastMessageDate\":\"2011-10-14T14:59:51-07:00\",\"dtoParentId\":270315,\"lastContactMobileNumber\":\"2069308934\",\"id\":1912,\"fingerprint\":\"2216445311\",\"new\":false,\"lastMessageBody\":\"yr cool! \",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-10-14T14:59:05-07:00\",\"cc\":\"\",\"deviceId\":270315}],\"sessions\":null,\"success\":true,\"size\":1}";
    public final static String USER_SAVE_RESULT = "{\"response\":{\"user\":{\"firstName\":\"Im\",\"lastName\":\"Cool\",\"mobileNumber\":\"2063758020\",\"fullName\":\"Im Cool\",\"phoneKey\":\"\",\"email\":\"\",\"notes\":\"\",\"birthday\":\"\",\"carrier\":\"Tmo\",\"loc\":\"\",\"dateCreated\":\"2011-10-14T14:57:35-07:00\",\"lastUpdated\":\"2011-10-17T13:40:01-07:00\"}},\"sessions\":null,\"success\":true}";
    public final static String MESSAGE_SEND_RESULT = "{\"response\":{\"fingerprint\":\"3969778241\",\"root\":\"7373193f-cb64-4e37-9ed6-a79d57fab524\",\"tokens\":[{\"message\":\"7373193f-cb64-4e37-9ed6-a79d57fab524\",\"fingerprint\":\"3969778241\",\"device\":270315,\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorToken\",\"contact\":-1}],\"class\":\"com.zipwhip.outgoing.distributor.OutgoingMessageDistributorResponse\"},\"sessions\":null,\"success\":true}";
    public final static String CONTACT_GET_RESPONSE = "{\"response\":{\"birthday\":null,\"state\":\"\",\"version\":4,\"dtoParentId\":270315,\"city\":\"\",\"id\":408050,\"phoneKey\":\"\",\"isZwUser\":false,\"vector\":\"\",\"thread\":\"20000002\",\"phoneId\":0,\"carrier\":\"Tmo\",\"firstName\":\"\",\"deviceId\":270315,\"lastName\":\"\",\"MOCount\":0,\"keywords\":\"\",\"zipcode\":\"\",\"ZOCount\":0,\"class\":\"com.zipwhip.website.data.dto.Contact\",\"lastUpdated\":\"2011-10-14T14:59:51-07:00\",\"loc\":\"\",\"targetGroupDevice\":-1,\"fwd\":\"20000102\",\"deleted\":false,\"latlong\":\"\",\"new\":false,\"email\":\"\",\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-10-14T14:58:54-07:00\",\"mobileNumber\":\"2063758020\",\"notes\":\"\",\"channel\":\"2\"},\"sessions\":null,\"success\":true}";

    @Before
    public void setUp() throws Exception {
        client = AsyncVendorClientFactory.createViaApiKey(apiKey, secret);
        client.setConnection(new MockApiConnection());
    }

    @Test
    public void testEnrollUser() throws Exception {
        ObservableFuture<EnrollmentResult> result = client.enrollUser(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNotNull(result.getResult());
        Assert.assertTrue(result.getResult().isCarbonEnabled());
        Assert.assertTrue(result.getResult().isCarbonInstalled());
        Assert.assertEquals(result.getResult().getDeviceNumber(), 999);
    }

    @Test
    public void testDeactivateUser() throws Exception {
        ObservableFuture<Void> result = client.deactivateUser(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testUserExists() throws Exception {
        ObservableFuture<Boolean> result = client.userExists(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNotNull(result.getResult());
        Assert.assertTrue(result.getResult());
    }

    @Test
    public void testSuggestCarbon() throws Exception {
        ObservableFuture<Void> result = client.suggestCarbon(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testCarbonInstalled() throws Exception {
        ObservableFuture<Boolean> result = client.carbonInstalled(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getResult());
    }

    @Test
    public void testCarbonEnabled() throws Exception {
        ObservableFuture<Boolean> result = client.carbonEnabled(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getResult());
    }

    @Test
    public void testSendMessage() throws Exception {
        ObservableFuture<List<MessageToken>> result = client.sendMessage(deviceAddress, Collections.singleton("5554443333"), "Hi mom");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNotNull(result.getResult());
        Assert.assertEquals(result.getResult().get(0).getMessage(), "7373193f-cb64-4e37-9ed6-a79d57fab524");
    }

    @Test
    public void testSaveUser() throws Exception {
        Contact user = new Contact();
        user.setFirstName("Im");
        user.setLastName("Cool");
        ObservableFuture<Contact> result = client.saveUser(deviceAddress, user);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().getFirstName(), "Im");
        Assert.assertEquals(result.getResult().getLastName(), "Cool");
    }

    @Test
    public void testReadMessages() throws Exception {
        Set<String> messages = new HashSet<String>();
        messages.add("123456");
        messages.add("654321");
        ObservableFuture<Void> result = client.readMessages(deviceAddress, messages);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testDeleteMessages() throws Exception {
        Set<String> messages = new HashSet<String>();
        messages.add("123456");
        messages.add("654321");
        ObservableFuture<Void> result = client.deleteMessages(deviceAddress, messages);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testReadConversation() throws Exception {
        ObservableFuture<Void> result = client.readConversation(deviceAddress, "123456");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testDeleteConversation() throws Exception {
        ObservableFuture<Void> result = client.deleteConversation(deviceAddress, "123456");
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testListConversations() throws Exception {
        ObservableFuture<List<Conversation>> result = client.listConversations(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().size(), 1);
        Assert.assertEquals(result.getResult().get(0).getDeviceAddress(), "device:/2063758020/0");
    }

    @Test
    public void testSaveContact() throws Exception {
        Contact contact = new Contact();
        contact.setFirstName("Dennis");
        contact.setLastName("Ritchie");
        contact.setAddress("ptn:/5555555555");
        ObservableFuture<Contact> result = client.saveContact(deviceAddress, contact);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().getId(), 408077L);
    }

    @Test
    public void testDeleteContact() throws Exception {
        Set<String> contacts = new HashSet<String>();
        contacts.add("123456");
        contacts.add("654321");
        ObservableFuture<Void> result = client.deleteContacts(deviceAddress, contacts);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertNull(result.getResult());
    }

    @Test
    public void testListContacts() throws Exception {
        ObservableFuture<List<Contact>> result = client.listContacts(deviceAddress);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().size(), 1);
        Assert.assertEquals(result.getResult().get(0).getId(), 408050L);
    }

    @Test
    public void testGetContact() throws Exception {
        ObservableFuture<Contact> result = client.getContact(deviceAddress, contactMobileNumber);
        Assert.assertNotNull(result);
        result.await();
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(result.getResult().getMobileNumber(), contactMobileNumber);
    }

    public class MockApiConnection extends DestroyableBase implements ApiConnection {

        @Override
        public ObservableFuture<String> send(String method, Map<String, Object> params) throws Exception {

            ObservableFuture<String> result = new DefaultObservableFuture<String>(this);

            if (ZipwhipNetworkSupport.USER_ENROLL.equalsIgnoreCase(method)) {
                result.setSuccess(ENROLLMENT_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.USER_DEACT.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.USER_EXISTS.equalsIgnoreCase(method)) {
                result.setSuccess(BOOLEAN_TRUE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CARBON_SUGGEST.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.MESSAGE_READ.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.MESSAGE_DELETE.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONVERSATION_READ.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONVERSATION_DELETE.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_DELETE.equalsIgnoreCase(method)) {
                result.setSuccess(VOID_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_LIST.equalsIgnoreCase(method)) {
                result.setSuccess(CONTACT_LIST_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_SAVE.equalsIgnoreCase(method)) {
                result.setSuccess(CONTACT_SAVE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONVERSATION_LIST.equalsIgnoreCase(method)) {
                result.setSuccess(CONVERSATION_LIST_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.USER_SAVE.equalsIgnoreCase(method)) {
                result.setSuccess(USER_SAVE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.MESSAGE_SEND.equalsIgnoreCase(method)) {
                result.setSuccess(MESSAGE_SEND_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CARBON_ENABLED_VENDOR.equalsIgnoreCase(method)) {
                result.setSuccess(BOOLEAN_TRUE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CARBON_INSTALLED.equalsIgnoreCase(method)) {
                result.setSuccess(BOOLEAN_TRUE_RESULT);
                return result;
            }
            if (ZipwhipNetworkSupport.CONTACT_GET.equalsIgnoreCase(method)) {
                result.setSuccess(CONTACT_GET_RESPONSE);
                return result;
            }

            return result;
        }

        @Override
        public void setSessionKey(String sessionKey) {

        }

        @Override
        public String getSessionKey() {
            return "";
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void setAuthenticator(SignTool authenticator) {

        }

        @Override
        public SignTool getAuthenticator() {
            return null;
        }

        @Override
        public void setHost(String host) {

        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public void setApiVersion(String apiVersion) {

        }

        @Override
        public String getApiVersion() {
            return null;
        }

        @Override
        protected void onDestroy() {

        }

        @Override
        public ObservableFuture<InputStream> sendBinaryResponse(String method, Map<String, Object> params) throws Exception {
            return null;
        }

    }

}