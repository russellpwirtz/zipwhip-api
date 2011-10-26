package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.dto.*;
import com.zipwhip.api.dto.EnrollmentResult;
import com.zipwhip.concurrent.NetworkFuture;

import java.util.*;

/**
 * You must be authenticated as a Vendor to use this API. It will reject your request if you are authenticated as a user.
 * <p/>
 * Provides administrator tools for a Vendor to connect to and communicate with Zipwhip.
 */
public interface AsyncVendorClient {

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
     * Enroll a user in Zipwhip. If the user is already enrolled the this call will have no effect.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to be enrolled in Zipwhip.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the enrollment.
     */
    NetworkFuture<EnrollmentResult> enrollUser(String deviceAddress);

    /**
     * Unenroll a user from Zipwhip. When a user is unenrolled their account is removed from Zipwhip.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to be unenrolled from Zipwhip.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deactivateUser(String deviceAddress);

    /**
     * Query Zipwhip to see if a user already exists.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user to query if they are enrolled in Zipwhip.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     *         The result will be {@code TRUE} if the user exists, {@code FALSE} otherwise.
     */
    NetworkFuture<Boolean> userExists(String deviceAddress);

    /**
     * This method will tell the Zipwhip Network to send an SMS to the users phone containing a URL. The URL is clicked
     * Zipwhip will detect if the user's phone is supported by Carbon, and if it is the user will be redirected to a
     * download of the Carbon software. If the user's phone is not supported an error message is presented to the user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to receive a DeviceCarbon link to their phone.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> suggestCarbon(String deviceAddress);

    /**
     * This method will query the Zipwhip Network to as to whether Device Carbon is installed on the user's phone.
     * Returns a {@code true} result if it is installed, otherwise {@code false}.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to receive a DeviceCarbon link to their phone.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Boolean> carbonInstalled(String deviceAddress);

    /**
     * This method will query the Zipwhip Network to as to whether Device Carbon is enabled on the user's phone.
     * Returns a {@code true} result if it is enabled, otherwise {@code false}.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user who wishes to receive a DeviceCarbon link to their phone.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Boolean> carbonEnabled(String deviceAddress);

    /**
     * Send a message via the Zipwhip network. The message is from the user represented by the {@code deviceAddress}.
     * The details of the message including the recipient is contained in the {@code Message} object.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param friendAddresses A list of mobile numbers of the recipients of the message.
     * @param body The text of the message to be sent.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<MessageToken>> sendMessage(String deviceAddress, Set<String> friendAddresses, String body);

    /**
     * Send a message via the Zipwhip network. The message is from the user represented by the {@code deviceAddress}.
     * The details of the message including the recipient is contained in the {@code Message} object.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param friendAddresses A list of mobile numbers of the recipients of the message.
     * @param body The text of the message to be sent.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<MessageToken>> sendMessage(String deviceAddress, String friendAddress, String body);

    /**
     *  List the messages for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<Message>> listMessages(String deviceAddress);

    /**
     * Save details for an existing user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param user The user to save or update.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     *         The result is the saved user.
     */
    NetworkFuture<Contact> saveUser(String deviceAddress, Contact user);

    /**
     * Mark a set of messages, identified by their UUIDs, for a given user as read.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param uuids         A list of message uuids for the messages to be marked as read.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> readMessages(String deviceAddress, Set<String> uuids);

    /**
     * Delete a set of messages, identified by their UUIDs, for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param uuids         A list of message uuids for the messages to be deleted.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deleteMessages(String deviceAddress, Set<String> uuids);

    /**
     * Mark a conversation identified by its fingerprint as read, for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param fingerprint  The fingerprint of the conversation to be marked as read.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> readConversation(String deviceAddress, String fingerprint);

    /**
     * Mark a set of messages, identified by their UUIDs, for a given user as read.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param fingerprint  The fingerprint of the conversation to be deleted.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deleteConversation(String deviceAddress, String fingerprint);

    /**
     * Query the list of conversations for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<Conversation>> listConversations(String deviceAddress);

    /**
     * Save the details of a contact for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param contact       The contact to be saved in the user's contact list.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Contact> saveContact(String deviceAddress, Contact contact);

    /**
     * Delete the details of a set of contacts for a given user.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param contactMobileNumbers A list of mobile numbers for the contacts to be deleted.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deleteContacts(String deviceAddress, Set<String> contactMobileNumbers);

    /**
     * Query a user's contact list.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<Contact>> listContacts(String deviceAddress);

    /**
     * Query a user's contact list for the specified contact.
     *
     * @param deviceAddress The device address (device:/5555555555/0) of the user.
     * @param contactMobileNumber The mobile number of the contact to be queried.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Contact> getContact(String deviceAddress, String contactMobileNumber);

}
