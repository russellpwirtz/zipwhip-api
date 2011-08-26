package com.zipwhip.api.signals;

import com.zipwhip.signals.presence.Presence;
import com.zipwhip.util.StringUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Austin
 * Date: 7/22/11
 * Time: 4:58 PM
 */
public class SignalsUtil {

    /**
     * Utility method to serialize a Presence object to a string according to the SignalServer protocol.
     *
     * @param presence The Presence object to be serialized
     * @return a string according to the SignalServer protocol
     */
    public static String serializePresence(Presence presence) {

        if(presence == null) {
            return StringUtil.EMPTY_STRING;
        }

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
        presenceMap.put("presenceStatus", presenceStatus);

        String connected = null;
        if(presence.getConnected() != null) {
            connected = presence.getConnected().toString();
        }
        presenceMap.put("connected", connected);

        presenceMap.put("subscriptionId", presence.getSubscriptionId());

        List<JSONObject> presences = new LinkedList<JSONObject>();
        presences.add(new JSONObject(presenceMap));

         JSONArray array = new JSONArray(presences);

        return array.toString();
    }

}
