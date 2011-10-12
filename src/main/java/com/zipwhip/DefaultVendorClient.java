package com.zipwhip;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.ZipwhipNetworkSupport;
import com.zipwhip.api.dto.DeviceToken;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.subscriptions.SubscriptionEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trusted Vendors will use this API to gain access to accounts and do various
 * system level requests.
 */
public class DefaultVendorClient extends ZipwhipNetworkSupport implements VendorClient {

    /**
     * Create a new DefaultVendorClient
     *
     * @param connection The connection to Zipwhip
     * @param signalProvider The SignalProvider to SignalServer
     */
    public DefaultVendorClient(ApiConnection connection, SignalProvider signalProvider) {
        super(connection, signalProvider);
    }

    @Override
    public List<MessageToken> sendVendorMessage(String address, String body) throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("address", address);
        params.put("body", body);
        params.put("apiKey", getConnection().getAuthenticator().apiKey);

        return responseParser.parseMessageTokens(executeSync(VENDOR_MESSAGE_SEND, params));
    }

    @Override
    public ApiConnection getConnection() {
        return connection;
    }

    @Override
    public DeviceToken getDeviceBySessionKey(String sessionKey) throws Exception {
        return null;
    }

    @Override
    public DeviceToken enrollDevice(String deviceAddress, SubscriptionEntry subscriptionEntry) throws Exception {

        List<SubscriptionEntry> subscriptionEntries = new ArrayList<SubscriptionEntry>();
        subscriptionEntries.add(subscriptionEntry);

        return enrollDevice(deviceAddress, subscriptionEntries);
    }

    @Override
    public DeviceToken enrollDevice(String deviceAddress) throws Exception {
        return enrollDevice(deviceAddress, (List<SubscriptionEntry>) null);
    }

    @Override
    public String lookupPhoneKey(String carrier, String carrierKey) throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("carrier", carrier);
        params.put("carrierKey", carrierKey);

        return responseParser.parseString(executeSync(PHONE_LOOKUP, params));
    }

    @Override
    public DeviceToken enrollDevice(String deviceAddress, List<SubscriptionEntry> subscriptionEntries) throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();

        // the device we want to modify
        params.put("address", deviceAddress);

        // our API key (so they can verify our signature)
        params.put("apiKey", (getConnection().getAuthenticator().apiKey));

        // the list of subscriptionEntries we want to add to the device
        if (subscriptionEntries != null) {
            for (SubscriptionEntry subscriptionEntry : subscriptionEntries) {
                params.put("subscription", subscriptionEntry.toString());
            }
        }

        return responseParser.parseDeviceToken(executeSync(USER_ENROLL, params));
    }

    @Override
    protected void onDestroy() {

    }
    
}
