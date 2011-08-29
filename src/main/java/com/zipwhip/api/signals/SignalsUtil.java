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

    public static List<Presence> parsePresence(String object) {

//        PresenceCommand command = new PresenceCommand();
//
//        JSONArray presenceList = null;
//        // try to find map in the presence key, if not, then try using the map directly
//        Object presenceObject = ParamUtil.getParam(properties, KEY_PRESENCE_ID);
//        if(presenceObject == null) {
//            return command;
//        }
//
//        String presenceJson = presenceObject.toString();
//        if(StringUtil.isNullOrEmpty(presenceJson)) {
//            return command;
//        }
//
//        presenceList = new JSONArray(presenceJson);
//        if (presenceList == null) {
//            return command;
//        }
//
//        PresenceGroup _presenceGroup = new PresenceGroup();
//
//
//        int len = presenceList.length();
//
//        for(int i = 0; i < len; i++){
//            Object obj = null;
//            try {
//                obj = presenceList.get(i);
//            } catch (JSONException e) {
//                e.printStackTrace();
//                continue;
//            }
//
//            Map presenceMap = (Map) obj;
//
//            Presence presence = new Presence();
//
//            presence.setConnected(ParamUtil.getBoolean(presenceMap, "connected"));
//
//            // subscriptionId can be NULL, its ok
//            presence.setSubscriptionId(ParamUtil.getString(presenceMap, "subscriptionId"));
//
//            String cat = ParamUtil.getString(presenceMap, "category");
//            if (StringUtil.isNullOrEmpty(cat)) {
//                presence.setCategory(PresenceCategory.NONE);
//            } else {
//                presence.setCategory(PresenceCategory.valueOf(cat));
//            }
//
//            String status = ParamUtil.getString(presenceMap, "status");
//            if (StringUtil.isNullOrEmpty(status)) {
//                // TODO assumed
//                presence.setStatus(PresenceStatus.OFFLINE);
//            } else {
//                presence.setStatus(PresenceStatus.valueOf(status));
//            }
//
//            Map _extraInfo = ParamUtil.getMap(presenceMap, "extraInfo");
//            if (_extraInfo != null) {
//                PresenceExtraInfo info = new PresenceExtraInfo();
//
//                Set keys = _extraInfo.keySet();
//                for (Object key : keys) {
//                    info.put((String) key, ParamUtil.getString(_extraInfo, (String) key));
//                }
//
//                presence.setExtraInfo(info);
//            }
//
//            Map _userAgent = ParamUtil.getMap(presenceMap, "userAgent");
//            if (_userAgent != null) {
//                UserAgent userAgent = new UserAgent();
//                userAgent.setMakeModel(ParamUtil.getString(_userAgent, "makeModel"));
//                userAgent.setBuild(ParamUtil.getString(_userAgent, "build"));
//
//                // product
//                Map _product = ParamUtil.getMap(_userAgent, "product");
//                if (_product != null) {
//                    Product product = new Product();
//                    String productName = ParamUtil.getString(_product, "name");
//                    if (!StringUtil.isNullOrEmpty(productName)) {
//                        product.setName(ProductLine.valueOf(productName));
//                    }
//                    product.setVersion(ParamUtil.getString(_product, "version"));
//                    product.setBuild(ParamUtil.getString(_product, "build"));
//                    userAgent.setProduct(product);
//                }
//
//                presence.setUserAgent(userAgent);
//            }
//
//            _presenceGroup.add(presence);
//        }
//
//        if (_presenceGroup.size() > 0) {
//            command.setPresenceGroup(_presenceGroup);
//        }

        return null;
    }

}
