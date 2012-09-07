package com.zipwhip.signals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.signals.presence.PresenceStatus;
import com.zipwhip.signals.presence.Product;
import com.zipwhip.signals.presence.ProductLine;
import com.zipwhip.signals.presence.UserAgent;
import com.zipwhip.util.Parser;
import com.zipwhip.util.Serializer;
import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * User: Austin
 * Date: 7/22/11
 * Time: 4:58 PM
 *
 * This class implements the parsing and serializing of Presence to and from a JSONArray.
 * The methods serialize and parse are symmetrical.
 */
public class PresenceUtil implements Parser<JSONArray, List<Presence>>, Serializer<List<Presence>, JSONArray> {

	private static Logger logger = Logger.getLogger(PresenceUtil.class);

	private static PresenceUtil instance;

	public static PresenceUtil getInstance() {
		if (instance == null) {
			instance = new PresenceUtil();
		}

		return instance;
	}

	/**
	 * Utility method to serialize a Presence object to a string according to the SignalServer protocol.
	 *
	 * @param presenceList The a list of Presence objects to be serialized
	 * @return a JSONArray parsed serialized according to the SignalServer protocol
	 */
	@Override
	public JSONArray serialize(List<Presence> presenceList) {

		if(presenceList == null) {
			return new JSONArray();
		}

		List<JSONObject> presences = new LinkedList<JSONObject>();

		for  (Presence presence : presenceList) {

			if(presence == null)
				continue;
			
			Map<String, Object> presenceMap = new HashMap<String, Object>();

			presenceMap.put("ip", presence.getIp());

			Map<String, Object> clientAddressMap = new HashMap<String, Object>();
			String clientId = null;
			if(presence.getAddress() != null) {
				clientId = presence.getAddress().getClientId();
			}
			clientAddressMap.put("clientId", clientId);
			presenceMap.put("address", new JSONObject(clientAddressMap));

			String category = null;
			if(presence.getCategory() != null) {
				category = presence.getCategory().toString();
			}
			presenceMap.put("category", category);

			String makeModel = null;
			String userAgentBuild = null;

			Map<String, Object> userAgentMap = new HashMap<String, Object>();
			Map<String, Object> productMap = new HashMap<String, Object>();

			if(presence.getUserAgent() != null) {

				makeModel = presence.getUserAgent().getMakeModel();
				userAgentBuild = presence.getUserAgent().getBuild();

				String name = null;
				String version = null;
				String productBuild = null;

				if(presence.getUserAgent().getProduct() != null) {
					name = presence.getUserAgent().getProduct().getName().toString();
					version = presence.getUserAgent().getProduct().getVersion();
					productBuild = presence.getUserAgent().getProduct().getBuild();
				}

				productMap.put("name", name);
				productMap.put("version", version);
				productMap.put("build", productBuild);
			}
			userAgentMap.put("makeModel", makeModel);
			userAgentMap.put("build", userAgentBuild);
			userAgentMap.put("product", new JSONObject(productMap));
			presenceMap.put("userAgent", new JSONObject(userAgentMap));

			String presenceStatus = null;
			if(presence.getStatus() != null) {
				presenceStatus = presence.getStatus().toString();
			}
			presenceMap.put("status", presenceStatus);

			String connected = null;
			if(presence.getConnected() != null) {
				connected = presence.getConnected().toString();
			}
			presenceMap.put("connected", connected);

			presenceMap.put("subscriptionId", presence.getSubscriptionId());


			presences.add(new JSONObject(presenceMap));

		}

		return new JSONArray(presences);
	}

	/**
	 * Utility method to deserialize a JSONArray into a List of Presence according to the SignalServer protocol.
	 *
	 * @param json A JSONArray of Presences according to the SignalServer protocol
	 * @return a List of Presence objects, if input is null, returns an empty list
	 */
	@Override
	public List<Presence> parse(JSONArray json) {

		List<Presence> presenceList = new ArrayList<Presence>();

		if (json == null) {
			return presenceList;
		}

		for (int i = 0; i < json.length(); i++) {

			try {
				JSONObject presenceJsonObject = json.getJSONObject(i);
				Presence presence = new Presence();

				// category
				String category;
                JSONObject categoryObject = presenceJsonObject.optJSONObject("category");

                if (categoryObject != null) {
                    category = categoryObject.optString("name");
                } else {
                    category = presenceJsonObject.optString("category");
                }

				if (StringUtil.isNullOrEmpty(category)) {
					presence.setCategory(PresenceCategory.NONE);
				} else {
					try {
						presence.setCategory(PresenceCategory.valueOf(category));
					} catch (Exception e) {
						logger.error("Error converting String to PresenceCategory", e);
					}
				}

				// userAgent
				JSONObject userAgentJsonObject = presenceJsonObject.optJSONObject("userAgent");
				if (userAgentJsonObject != null) {

					UserAgent userAgent = new UserAgent();
					userAgent.setBuild(userAgentJsonObject.optString("build", StringUtil.EMPTY_STRING));
					userAgent.setMakeModel(userAgentJsonObject.optString("makeModel", StringUtil.EMPTY_STRING));

					JSONObject productJsonObject = userAgentJsonObject.optJSONObject("product");
					if (productJsonObject != null) {

						Product product = new Product();
						product.setBuild(productJsonObject.optString("build", StringUtil.EMPTY_STRING));
						product.setVersion(productJsonObject.optString("version", StringUtil.EMPTY_STRING));

                        // name
                        String name;
                        JSONObject nameObject = productJsonObject.optJSONObject("name");

                        if (nameObject != null) {
                            name = nameObject.optString("name");
                        } else {
                            name = productJsonObject.optString("name");
                        }

						if (StringUtil.exists(name)) {
							try {
								product.setName(ProductLine.valueOf(name));
							} catch (Exception e) {
								logger.error("Error converting String to ProductLine", e);
							}
						}

						userAgent.setProduct(product);
					}

					presence.setUserAgent(userAgent);
				}

				// address
				JSONObject clientAddressJsonObject = presenceJsonObject.optJSONObject("address");
				if (clientAddressJsonObject != null) {

					ClientAddress address = new ClientAddress();
					address.setClientId(clientAddressJsonObject.optString("clientId", StringUtil.EMPTY_STRING));

					presence.setAddress(address);
				}

				// presenceStatus
                String status;
                JSONObject statusObject = presenceJsonObject.optJSONObject("status");

                if (statusObject != null) {
                    status = statusObject.optString("name");
                } else {
                    status = presenceJsonObject.optString("status");
                }

				if (StringUtil.isNullOrEmpty(status)) {
					presence.setStatus(PresenceStatus.OFFLINE);
				} else {
					presence.setStatus(PresenceStatus.valueOf(status));
				}

				// connected
				presence.setConnected(presenceJsonObject.optBoolean("connected", false));

				// subscriptionId
				presence.setSubscriptionId(presenceJsonObject.optString("subscriptionId", StringUtil.EMPTY_STRING));

				// ip
				presence.setIp(presenceJsonObject.optString("ip", StringUtil.EMPTY_STRING));

				// Add to the list
				presenceList.add(presence);

			} catch (JSONException e) {
				logger.error("Error parsing Presence", e);
			}
		}

		return presenceList;
	}

}
