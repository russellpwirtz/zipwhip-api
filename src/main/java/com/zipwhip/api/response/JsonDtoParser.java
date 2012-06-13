package com.zipwhip.api.response;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zipwhip.api.dto.BasicDto;
import com.zipwhip.api.dto.CarbonEvent;
import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Conversation;
import com.zipwhip.api.dto.Device;
import com.zipwhip.api.dto.Message;
import com.zipwhip.api.dto.MessageAttachment;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.dto.TransmissionState;
import com.zipwhip.api.dto.User;
import com.zipwhip.util.JsonDateUtil;
import com.zipwhip.util.Parser;
import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: Michael
 * Date: 7/6/11
 * Time: 3:04 PM
 * <p/>
 * Centralizes the act of parsing DTO's from JSON.
 */
public class JsonDtoParser {

	public final Parser<JSONObject, Message> MESSAGE_PARSER = new Parser<JSONObject, Message>() {
		@Override
		public Message parse(JSONObject jsonObject) throws Exception {
			return parseMessage(jsonObject);
		}
	};

	public final Parser<JSONObject, Contact> CONTACT_PARSER = new Parser<JSONObject, Contact>() {
		@Override
		public Contact parse(JSONObject jsonObject) throws Exception {
			return parseContact(jsonObject);
		}
	};

	public final Parser<JSONObject, Conversation> CONVERSATION_PARSER = new Parser<JSONObject, Conversation>() {
		@Override
		public Conversation parse(JSONObject jsonObject) throws Exception {
			return parseConversation(jsonObject);
		}
	};

	public final Parser<JSONObject, CarbonEvent> CARBON_PARSER = new Parser<JSONObject, CarbonEvent>() {
		@Override
		public CarbonEvent parse(JSONObject jsonObject) throws Exception {
			return parseCarbonEvent(jsonObject);
		}
	};

	public final Parser<JSONObject, Device> DEVICE_PARSER = new Parser<JSONObject, Device>() {
		@Override
		public Device parse(JSONObject jsonObject) throws Exception {
			return parseDevice(jsonObject);
		}
	};

	public final Parser<JSONObject, User> USER_PARSER = new Parser<JSONObject, User>() {
		@Override
		public User parse(JSONObject jsonObject) throws Exception {
			return parseUser(jsonObject);
		}
	};

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

		contact.setDeviceId(parseLong(content.optString("deviceId")));
		contact.setAddress(content.optString("address"));
		contact.setMobileNumber(content.optString("mobileNumber"));
		contact.setState(content.optString("state"));
		contact.setCity(content.optString("city"));
		contact.setId(parseLong(content.optString("id")));
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
		contact.setLoc(content.optString("loc"));

		return contact;
	}

	/**
	 * Parse a Contact from a JSONObject if the object contains it.
	 *
	 * @param content JSONObject to be parsed.
	 * @return A Contact object parsed from the JSON content.
	 * @throws JSONException If an error is encountered while parsing
	 */
	public User parseUser(JSONObject content) throws JSONException {

		if (content == null) {
			return null;
		}

		User user = parseBasicDto(new User(), content);

		user.setMobileNumber(content.optString("mobileNumber"));
		user.setPhoneKey(content.optString("phoneKey"));
		user.setCarrier(content.optString("carrier"));
		user.setFirstName(content.optString("firstName"));
		user.setLastName(content.optString("lastName"));
		user.setMoCount(content.optInt("MOCount"));
		user.setZoCount(content.optInt("ZOCount"));
		user.setZipcode(content.optString("zipcode"));
		user.setEmail(content.optString("email"));
		user.setNotes(content.optString("notes"));
		user.setLoc(content.optString("loc"));
		user.setWebsiteDeviceId(parseLong(content.optString("websiteDeviceId")));

		return user;
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

		JSONObject transmissionStateJson = response.optJSONObject("transmissionState");
		if (transmissionStateJson != null) {
			message.setTransmissionState(TransmissionState.parse(transmissionStateJson.optString("name")));
		}

		message.setId(Long.parseLong(response.optString("id")));
		message.setUuid(response.optString("uuid"));
		message.setDeviceId(parseLong(response.optString("deviceId")));
		message.setContactId(parseLong(response.optString("contactId")));
		message.setContactDeviceId(parseLong(response.optString("contactDeviceId")));
		message.setAddress(response.optString("address"));
		message.setRead(response.optBoolean("isRead"));
		message.setDeleted(response.optBoolean("deleted"));
		message.setFingerprint(response.optString("fingerprint"));
		message.setCc(response.optString("cc"));
		message.setBcc(response.optString("bcc"));
		message.setErrorState(response.optBoolean("errorState"));
		message.setBody(StringUtil.stripStringNull(response.optString("body")));
		message.setDateCreated(JsonDateUtil.getDate(response.optString("dateCreated")));
		message.setLastUpdated(JsonDateUtil.getDate(response.optString("lastUpdated")));
		message.setSourceAddress(response.optString("sourceAddress"));
		message.setDestinationAddress(response.optString("destAddress"));
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
		message.setMessageType(response.optString("type"));
		message.setAdvertisement(response.optString("advertisement"));
		message.setHasAttachment(response.optBoolean("hasAttachment"));

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

			token.setMessage(json.optString("message"));
			token.setDeviceId(parseLong(json.optString("device"))); // will be 0 if it is a self message
			token.setContactId(parseLong(json.optString("contact"))); // will be 0 if it is a self message
			token.setFingerprint(json.optString("fingerprint"));

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

		conversation.setId(Long.parseLong(content.optString("id")));
		conversation.setDeviceId(parseLong(content.optString("deviceId")));
		conversation.setDeviceAddress(content.optString("deviceAddress"));
		conversation.setFingerprint(content.optString("fingerprint"));
		conversation.setAddress(content.optString("address"));
		conversation.setCc(content.optString("cc"));
		conversation.setBcc(content.optString("bcc"));
		conversation.setUnreadCount(content.optInt("unreadCount"));
		conversation.setLastContactId(parseLong(content.optString("lastContactId")));
		conversation.setNew(content.optBoolean("new"));
		conversation.setDeleted(content.optBoolean("deleted"));
		conversation.setVersion(content.optInt("version"));
		conversation.setLastContactDeviceId(parseLong(content.optString("lastContactDeviceId")));
		conversation.setLastMessageBody(StringUtil.stripStringNull(content.optString("lastMessageBody")));
		conversation.setLastContactFirstName(StringUtil.stripStringNull(content.optString("lastContactFirstName")));
		conversation.setLastContactLastName(StringUtil.stripStringNull(content.optString("lastContactLastName")));
		conversation.setLastContactMobileNumber(StringUtil.stripStringNull(content.optString("lastContactMobileNumber")));
		conversation.setLastMessageDate(JsonDateUtil.getDate(content.optString("lastMessageDate")));
		conversation.setLastNonDeletedMessageDate(JsonDateUtil.getDate(content.optString("lastNonDeletedMessageDate")));
		conversation.setDateCreated(JsonDateUtil.getDate(content.optString("dateCreated")));
		conversation.setLastUpdated(JsonDateUtil.getDate(content.optString("lastUpdated")));

		return conversation;
	}

	/**
	 * Parse a MessageAttachment from a JSONObject if the object contains it.
	 *
	 * @param content JSONObject to be parsed.
	 * @return A MessageAttachment object parsed from the JSON content.
	 * @throws JSONException If an error is encountered while parsing
	 */
	public MessageAttachment parseMessageAttachment(JSONObject content) throws JSONException {

		if (content == null) {
			return null;
		}

		MessageAttachment attachment = new MessageAttachment();

		attachment.setDateCreated(JsonDateUtil.getDate(content.optString("dateCreated")));
		attachment.setDeviceId(parseLong(content.optString("deviceId")));
		attachment.setId(Long.parseLong(content.optString("id")));
		attachment.setLastUpdated(JsonDateUtil.getDate(content.optString("lastUpdated")));
		attachment.setMessageId(parseLong(content.optString("messageId")));
		attachment.setMimeType(content.optString("mimeType"));
		attachment.setNew(content.optBoolean("new"));
		attachment.setStorageKey(content.optString("storageKey"));
		attachment.setVersion(content.optLong("version"));

		return attachment;
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

		device.setId(Long.parseLong(response.optString("id")));
		device.setUuid(response.optString("uuid"));
		device.setAddress(response.optString("address"));
		device.setThread(response.optString("thread"));
		device.setVersion(response.optInt("version"));
		device.setLastUpdated(JsonDateUtil.getDate(response.optString("lastUpdated")));
		device.setDateCreated(JsonDateUtil.getDate(response.optString("dateCreated")));
		device.setUserId(parseLong(response.optString("userId")));
		device.setChannel(response.optString("channel"));
		device.setTextline(response.optString("textline"));
		device.setDisplayName(response.optString("displayName"));

		return device;
	}

	public CarbonEvent parseCarbonEvent(JSONObject content) throws JSONException {

		if (content == null) {
			return null;
		}

		CarbonEvent carbonEvent = new CarbonEvent();

		carbonEvent.carbonDescriptor = content.optString("carbonDescriptor");

		return carbonEvent;
	}

	private <T extends BasicDto> T parseBasicDto(T dto, JSONObject content) {
		dto.setLastUpdated(JsonDateUtil.getDate(content.optString("lastUpdated")));
		dto.setDateCreated(JsonDateUtil.getDate(content.optString("dateCreated")));
		dto.setVersion(content.optLong("version"));

		return dto;
	}

	private long parseLong(String opt) {
		if((opt == null) || "".equals(opt)) {
			return 0l;
		} else {
			try {
				return Long.parseLong(opt);
			} catch(NumberFormatException e) { return 0l; }
		}
	}
}
