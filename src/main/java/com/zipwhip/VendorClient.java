package com.zipwhip;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.dto.DeviceToken;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.subscriptions.SubscriptionEntry;

import java.util.List;

/**
 * You must be authenticated as a Vendor to use this API. It will reject
 * your request if you are authenticated as a user.
 * <p/>
 *
 * Provides administrator tools for a Vendor to connect to and communicate with Zipwhip.
 */
public interface VendorClient {

    /**
     * Set the connection to the Zipwhip REST API.
     *
     * @param connection A transport to Zipwhip.
     */
    void setConnection(ApiConnection connection);

    /**
     * Get the connection to the Zipwhip REST API.
     *
     * @return A transport to Zipwhip.
     */
    ApiConnection getConnection();

    /**
     * You must be authenticated as a Vendor to use this method. It will reject
     * your request if you are authenticated as a user.
     * <p/>
     *
     * @param deviceAddress The address of the destination you want to send to. In this
     *        version make sure you're only sending to ptn:/ style addresses.
     * @param body The body you want to send to the customer. We won't put on an
     *        advertisement since this is a system message.
     * @return A list of tokens indicating the state of the sent message.
     * @throws Exception If an error occurred while sending the message or parsing the response.
     */
    List<MessageToken> sendVendorMessage(String deviceAddress, String body) throws Exception;

    /**
     * You must be authenticated as a Vendor to use this method. It will reject
     * your request if you are authenticated as a user.
     * <p/>
     * 1. If this device does not exist, it will be created. 2. The device will
     * be subscribed to ExternalAPI if it is not already. (You will be given the
     * secret, so that you can execute calls on behalf of the acct.) 3. The
     * device will be subscribed to the subscriptions you specify. 4. If you do
     * not provide SubscriptionSettings, the defaults will be used.
     *
     * @param deviceAddress The user account you want to enroll.
     * @return The details of the modified account.
     * @throws Exception If an error occurred while enrolling the device or parsing the response.
     */
    DeviceToken enrollDevice(String deviceAddress) throws Exception;

    /**
     * You must be authenticated as a Vendor to use this method. It will reject
     * your request if you are authenticated as a user.
     * <p/>
     * 1. If this device does not exist, it will be created. 2. The device will
     * be subscribed to ExternalAPI if it is not already. (You will be given the
     * secret, so that you can execute calls on behalf of the acct.) 3. The
     * device will be subscribed to the subscriptions you specify. 4. If you do
     * not provide SubscriptionSettings, the defaults will be used.
     *
     * @param deviceAddress The user account you want to enroll.
     * @param subscriptionEntry The subscriptionEntry to enroll.
     * @return The details of the modified account.
     * @throws Exception If an error occurred while enrolling the device or parsing the response.
     */
    DeviceToken enrollDevice(String deviceAddress, SubscriptionEntry subscriptionEntry) throws Exception;

    /**
     * You must be authenticated as a Vendor to use this method. It will reject
     * your request if you are authenticated as a user.
     * <p/>
     * 1. If this device does not exist, it will be created. 2. The device will
     * be subscribed to ExternalAPI if it is not already. (You will be given the
     * secret, so that you can execute calls on behalf of the acct.) 3. The
     * device will be subscribed to the subscriptionEntries you specify. 4. If you do
     * not provide SubscriptionSettings, the defaults will be used.
     *
     * @param deviceAddress The user account you want to enroll.
     * @param subscriptionEntries A list of subscriptionEntries to enroll.
     * @return The details of the modified account.
     * @throws Exception If an error occurred while enrolling the device or parsing the response.
     */
    DeviceToken enrollDevice(String deviceAddress, List<SubscriptionEntry> subscriptionEntries) throws Exception;

    /**
     * You must be authenticated as a Vendor to use this method. It will reject
     * your request if you are authenticated as a user.
     * <p/>
     *
     * @param deviceAddress
     * @return True iof the device is enrolled, otherwise false.
     * @throws Exception If an error occurred while querying the device or parsing the response.
     */
//    boolean isEnrolled(String deviceAddress) throws Exception;

    /**
     * You must be authenticated as a Vendor to use this method. It will reject
     * your request if you are authenticated as a user.
     * <p/>
     *
     * @param sessionKey The sessionKey of the device's current session.
     * @return A token representing the device.
     * @throws Exception If an error occurred getting the device or parsing the response.
     */
    DeviceToken getDeviceBySessionKey(String sessionKey) throws Exception;

    /**
     * You must be authenticated as a Vendor to use this method. It will reject
     * your request if you are authenticated as a user.
     * <p/>
     * Lookup a phone key by the carrierKey. The carrierKey is the phone info from that carrier's LDAP.
     *
     * @param carrier    Examples include Vzw, Tmo, Sprint
     * @param carrierKey The phoneKey as defined by the carrier. MOTO-z83 (this is proprietary per carrier)
     * @return The standardized Zipwhip phoneKey that maps to an image set.
     * @throws Exception If an error occurred doing the lookup or parsing the response.
     */
    String lookupPhoneKey(String carrier, String carrierKey) throws Exception;

}
