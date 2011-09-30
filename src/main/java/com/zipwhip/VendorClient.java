package com.zipwhip;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.dto.DeviceToken;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.subscriptions.Subscription;

import java.util.List;

/**
 * Provides administrator tools for a Vendor
 */
public interface VendorClient {

    /**
     *
     * @param connection
     */
    void setConnection(ApiConnection connection);

    /**
     *
     * @return
     */
    ApiConnection getConnection();

    /**
     *
     * @param address
     * @param body
     * @return
     * @throws Exception
     */
    List<MessageToken> sendVendorMessage(String address, String body) throws Exception;

    /**
     *
     * @param deviceAddress
     * @param subscription
     * @return
     * @throws Exception
     */
    DeviceToken enrollDevice(String deviceAddress, Subscription subscription) throws Exception;

    /**
     *
     * @param deviceAddress
     * @return
     * @throws Exception
     */
    DeviceToken enrollDevice(String deviceAddress) throws Exception;

    /**
     *
     * @param deviceAddress
     * @param subscriptions
     * @return
     * @throws Exception
     */
    DeviceToken enrollDevice(String deviceAddress, List<Subscription> subscriptions) throws Exception;

    /**
     *
     * @param sessionKey
     * @return
     * @throws Exception
     */
    DeviceToken getDeviceBySessionKey(String sessionKey) throws Exception;

    /**
     * Lookup a phone key by the carrierKey. The carrierKey is the phone info from that carrier's LDAP.
     *
     * @param carrier    Examples include Vzw, Tmo, Sprint
     * @param carrierKey The phoneKey as defined by the carrier. MOTO-z83 (this is proprietary per carrier)
     * @return The standardized Zipwhip phoneKey that maps to an image set.
     * @throws Exception
     */
    String lookupPhoneKey(String carrier, String carrierKey) throws Exception;

}
