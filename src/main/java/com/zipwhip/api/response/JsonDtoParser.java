package com.zipwhip.api.response;

import com.zipwhip.api.dto.*;
import com.zipwhip.lib.DateUtil;
import com.zipwhip.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 7/6/11
 * Time: 3:04 PM
 * <p/>
 * Centralizes the act of parsing DTO's from JSON.
 */
public class JsonDtoParser {

    private static JsonDtoParser instance;

    public static JsonDtoParser getInstance() {
        if (instance == null) {
            instance = new JsonDtoParser();
        }

        return instance;
    }

    private <T extends BasicDto> T parseBasicDto(T dto, JSONObject content) {
        dto.setLastUpdated(DateUtil.safeParseJsonDate(content.optString("lastUpdated")));
        dto.setDateCreated(DateUtil.safeParseJsonDate(content.optString("dateCreated")));
        dto.setVersion(content.optInt("version"));

        return dto;
    }

    /**
     * Parse a Contact from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A Contact object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Contact parseContact(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        Contact contact = parseBasicDto(new Contact(), content);

        contact.setDeviceId(content.optLong("deviceId"));
        contact.setAddress(content.optString("address"));
        contact.setMobileNumber(content.optString("mobileNumber"));
        contact.setState(content.optString("state"));
        contact.setCity(content.optString("city"));
        contact.setId(content.optLong("id"));
        contact.setPhoneKey(content.optString("phoneKey"));
        contact.setThread(content.optString("thread"));
        contact.setFwd(content.optString("fwd"));
        contact.setCarrier(content.optString("carrier"));
        contact.setFirstName(content.optString("firstName"));
        contact.setLastName(content.optString("lastName"));
        contact.setMoCount(content.optInt("MOCount"));
        contact.setZoCount(content.optInt("ZOCount"));
        contact.setZipcode(content.optString("zipcode"));
        contact.setLatlong(content.optString("latlong"));
        contact.setEmail(content.optString("email"));
        contact.setNotes(content.optString("notes"));
        contact.setChannel(content.optString("channel"));

        return contact;
    }

    /**
     * Parse a Message from a JSONObject if the object contains it.
     *
     * @param response JSONObject to be parsed.
     * @return A Message object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Message parseMessage(JSONObject response) throws JSONException {

        if (response == null) {
            return null;
        }

        Message message = new Message();

        message.setUuid(response.optString("uuid"));
        message.setDeviceId(response.optLong("deviceId"));
        message.setContactId(response.optLong("contactId"));
        message.setContactDeviceId(response.optLong("contactDeviceId"));
        message.setAddress(response.optString("address"));
        message.setRead(response.optBoolean("isRead"));
        message.setDeleted(response.optBoolean("deleted"));
        message.setFingerprint(response.optString("fingerprint"));
        message.setCc(response.optString("cc"));
        message.setBcc(response.optString("bcc"));
        message.setErrorState(response.optBoolean("errorState"));
        message.setBody(StringUtil.stripStringNull(response.optString("body")));
        message.setDateCreated(DateUtil.safeParseJsonDate(response.optString("dateCreated")));
        message.setLastUpdated(DateUtil.safeParseJsonDate(response.optString("lastUpdated")));
        message.setSourceAddress(response.optString("sourceAddress"));
        message.setDestAddress(response.optString("destAddress"));
        message.setStatusCode(response.optInt("statusCode"));
        message.setStatusDesc(response.optString("statusDesc"));
        message.setThread(response.optString("thread"));
        message.setChannel(response.optString("channel"));
        message.setFwd(response.optString("fwd"));
        message.setCarrier(response.optString("carrier"));
        message.setSubject(response.optString("subject"));
        message.setTo(response.optString("to"));
        message.setMobileNumber(response.optString("mobileNumber"));
        message.setFirstName(response.optString("firstName"));
        message.setLastName(response.optString("lastName"));
        message.setVersion(response.optLong("version"));

        return message;
    }

    /**
     * Parse a List<MessageToken> from a JSONObject if the object contains it.
     *
     * @param array JSONObject to be parsed.
     * @return A CList<MessageToken>ontact object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public List<MessageToken> parseMessageTokens(JSONArray array) throws JSONException {

        List<MessageToken> result = new ArrayList<MessageToken>();
        int len = array.length();
        for (int i = 0; i < len; i++) {

            JSONObject json = array.getJSONObject(i);
            MessageToken token = new MessageToken();

            token.setMessage(json.getString("message"));
            token.setDeviceId(json.optLong("device")); // will be 0 if it is a self message
            token.setContactId(json.optLong("contact")); // will be 0 if it is a self message

            result.add(token);
        }

        return result;
    }

    /**
     * Parse a Conversation from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A Conversation object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Conversation parseConversation(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        Conversation conversation = new Conversation();

        conversation.setId(content.optInt("id"));
        conversation.setDeviceId(content.optLong("deviceId"));
        conversation.setDeviceAddress(content.optString("deviceAddress"));
        conversation.setFingerprint(content.optString("fingerprint"));
        conversation.setAddress(content.optString("address"));
        conversation.setCc(content.optString("cc"));
        conversation.setBcc(content.optString("bcc"));
        conversation.setUnreadCount(content.optInt("unreadCount"));
        conversation.setLastContactId(content.optLong("lastContactId"));
        conversation.setNew(content.optBoolean("new"));
        conversation.setDeleted(content.optBoolean("deleted"));
        conversation.setVersion(content.optInt("version"));
        conversation.setLastContactDeviceId(content.optLong("lastContactDeviceId"));
        conversation.setLastMessageBody(StringUtil.stripStringNull(content.optString("lastMessageBody")));
        conversation.setLastContactFirstName(StringUtil.stripStringNull(content.optString("lastContactFirstName")));
        conversation.setLastContactLastName(StringUtil.stripStringNull(content.optString("lastContactLastName")));
        conversation.setLastContactMobileNumber(StringUtil.stripStringNull(content.optString("lastContactMobileNumber")));

        conversation.setLastMessageDate(DateUtil.safeParseJsonDate(content.optString("lastMessageDate")));
        conversation.setLastNonDeletedMessageDate(DateUtil.safeParseJsonDate(content.optString("lastNonDeletedMessageDate")));

        return conversation;
    }

    /**
     * Parse a Device from a JSONObject if the object contains it.
     *
     * @param response JSONObject to be parsed.
     * @return A Device object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public Device parseDevice(JSONObject response) throws JSONException {

        if (response == null) {
            return null;
        }

        Device device = new Device();

        device.setId(response.optInt("id"));
        device.setUuid(response.optString("uuid"));
        device.setAddress(response.optString("address"));
        device.setThread(response.optString("thread"));
        device.setVersion(response.optInt("version"));
        device.setLastUpdated(DateUtil.safeParseJsonDate(response.optString("lastUpdated")));
        device.setDateCreated(DateUtil.safeParseJsonDate(response.optString("dateCreated")));
        device.setUserId(response.optLong("userId"));
        device.setChannel(response.optString("channel"));
        device.setTextline(response.optString("textline"));
        device.setDisplayName(response.optString("displayName"));

        return device;
    }

    /**
     * Parse a MessageProgress from a JSONObject if the object contains it.
     *
     * @param content JSONObject to be parsed.
     * @return A MessageProgress object parsed from the JSON content.
     * @throws JSONException If an error is encountered while parsing
     */
    public MessageProgress parseMessageProgress(JSONObject content) throws JSONException {

        if (content == null) {
            return null;
        }

        MessageProgress messageProgress = new MessageProgress();

        int valueInt = content.optInt("value", Integer.MIN_VALUE);

        if (valueInt != Integer.MIN_VALUE) {

            messageProgress.setCode(valueInt);

        } else {

            JSONObject value = content.optJSONObject("value");

            if (value != null) {
                messageProgress.setDesc(value.optString("desc"));
                messageProgress.setCode(value.optInt("code"));
            }
        }

        messageProgress.setKey(content.optString("key"));

        return messageProgress;
    }

    public CarbonEvent parseCarbonMessageContent(JSONObject content) throws JSONException {

        if(content == null) {
            return null;
        }

        CarbonEvent carbonEvent = new CarbonEvent();

        carbonEvent.carbonDescriptor = content.optString("carbonDescriptor");

        return carbonEvent;
    }

}
