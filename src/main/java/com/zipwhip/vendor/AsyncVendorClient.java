package com.zipwhip.vendor;

import com.zipwhip.api.ApiConnection;
import com.zipwhip.api.dto.Contact;
import com.zipwhip.api.dto.Conversation;
import com.zipwhip.api.dto.Message;
import com.zipwhip.api.dto.MessageToken;
import com.zipwhip.api.response.EnrollmentResult;
import com.zipwhip.concurrent.NetworkFuture;

import java.util.List;
import java.util.Set;

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
     * @param mobileNumber The mobile number of the user who wishes to be enrolled in Zipwhip.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the enrollment.
     */
    NetworkFuture<EnrollmentResult> enrollUser(String mobileNumber);

    /**
     * Unenroll a user from Zipwhip. When a user is unenrolled their account is removed from Zipwhip.
     *
     * @param mobileNumber The mobile number of the user who wishes to be unenrolled from Zipwhip.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deactivateUser(String mobileNumber);

    /**
     * Query Zipwhip to see if a user already exists.
     *
     * @param mobileNumber The mobile number of the user to query if they are enrolled in Zipwhip.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     *         The result will be {@code TRUE} if the user exists, {@code FALSE} otherwise.
     */
    NetworkFuture<Boolean> userExists(String mobileNumber);

    /**
     * This method will tell the Zipwhip Network to send an SMS to the users phone containing a URL. The URL is clicked
     * Zipwhip will detect if the user's phone is supported by Carbon, and if it is the user will be redirected to a
     * download of the Carbon software. If the user's phone is not supported an error message is presented to the user.
     *
     * @param mobileNumber The mobile number of the user who wishes to receive a DeviceCarbon link to their phone.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> suggestCarbon(String mobileNumber);

    /**
     * Send a message via the Zipwhip network. The message is from the user represented by the {@code mobileNumber}.
     * The details of the message including the recipient is contained in the {@code Message} object.
     *
     * @param mobileNumber The mobile number of the user.
     * @param message       The message to send.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<MessageToken>> sendMessage(String mobileNumber, Message message);

    /**
     *
     *
     * @param mobileNumber The mobile number of the user.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<Message>> listMessages(String mobileNumber);

    /**
     *
     *
     * @param user The user to save or update.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     *         The result is the saved user.
     */
    NetworkFuture<Contact> saveUser(Contact user);

    /**
     * @param mobileNumber The mobile number of the user.
     * @param uuids         A list of message uuids for the messages to be marked as read.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> readMessages(String mobileNumber, Set<String> uuids);

    /**
     * @param mobileNumber The mobile number of the user.
     * @param uuids         A list of message uuids for the messages to be deleted.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deleteMessages(String mobileNumber, Set<String> uuids);

    /**
     * @param mobileNumber The mobile number of the user.
     * @param fingerprints  A list of conversation fingerprints for the conversations to be marked as read.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> readConversations(String mobileNumber, Set<String> fingerprints);

    /**
     *
     *
     * @param mobileNumber The mobile number of the user.
     * @param uuids         A list of conversation uuids for the conversations to be deleted.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deleteConversations(String mobileNumber, Set<String> uuids);

    /**
     *
     *
     * @param mobileNumber The mobile number of the user.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<Conversation>> listConversations(String mobileNumber);

    /**
     *
     *
     * @param mobileNumber The mobile number of the user.
     * @param contact       The contact to be saved in the user's contact list.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Contact> saveContact(String mobileNumber, Contact contact);

    /**
     *
     *
     * @param mobileNumber The mobile number of the user.
     * @param contactIds    A list of ids for the contacts to be deleted.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<Void> deleteContact(String mobileNumber, Set<Long> contactIds);

    /**
     *
     *
     * @param mobileNumber The mobile number of the user.
     * @return A {@code NetworkFuture} that will asynchronously report the result of the call.
     */
    NetworkFuture<List<Contact>> listContacts(String mobileNumber);

}
