package com.zipwhip.api.signals;

import com.google.gson.Gson;
import com.zipwhip.api.signals.dto.DeliveredMessage;
import com.zipwhip.api.signals.dto.json.SignalProviderGsonBuilder;
import com.zipwhip.signals2.address.ClientAddress;
import com.zipwhip.signals2.message.DefaultMessage;
import com.zipwhip.signals2.message.Message;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Date: 9/10/13
 * Time: 10:56 AM
 *
 * @author Michael
 * @version 1
 */
public class GsonParserTest {

    private Gson gson = SignalProviderGsonBuilder.getInstance();

    @Test
    public void testDevice() throws Exception {

    }

    @Test
    public void testConversation() throws Exception {

    }

    @Test
    public void testMessage() throws Exception {
        Map<String, Object> deliveredMessageMap = new HashMap<String, Object>();
        Map<String, Object> content = new HashMap<String, Object>();
        Map<String, Object> msg = new HashMap<String, Object>();
        deliveredMessageMap.put("message", msg);

        long timestamp = System.currentTimeMillis();
        long id = 34234234;

        content.put("body", "body");
        content.put("dateRead", 3242345345L);
        content.put("advertisement", "advertisement");
        content.put("transmissionState", "DELIVERED");
        content.put("contactDeviceId", 234234L);
        content.put("contactId", 34234L);
        content.put("messageType", "MO");
        content.put("scheduledDate", null);
        content.put("fingerprint", "fingerprint");
        content.put("address", "ptn:/23123123");
        content.put("dateCreated", 324234324L);
        content.put("attachments", true);
        content.put("fromName", "fromName");
        content.put("deviceId", 234243L);

        msg.put("content", content);
        msg.put("address", new ClientAddress("clientId"));
        msg.put("timestamp", timestamp);
        msg.put("id", id);
        msg.put("type", "message");
        msg.put("event", "receive");

        String json = gson.toJson(deliveredMessageMap);

        DeliveredMessage deliveredMessage = gson.fromJson(json, DeliveredMessage.class);
        Message message = deliveredMessage.getMessage();

        message.getTimestamp();
        message.getEvent();
        message.getType();
        message.getId();
        message.getContent();


    }

    @Test
    public void testContact() throws Exception {
        String json = "";

        DefaultMessage defaultMessage = new DefaultMessage();
        defaultMessage.setTimestamp(23423423L);
        defaultMessage.setAddress(new ClientAddress("clientId"));
        defaultMessage.setEvent("event");
        defaultMessage.setType("type");
        defaultMessage.setId("id");


        DeliveredMessage message = new DeliveredMessage();
        message.setSubscriptionIds(new TreeSet<String>(Arrays.asList("subscriptionId1")));
        message.setMessage(new DefaultMessage());

        gson.fromJson(json, DeliveredMessage.class);

    }
}
