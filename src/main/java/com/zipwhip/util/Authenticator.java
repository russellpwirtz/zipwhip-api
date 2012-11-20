package com.zipwhip.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 18, 2009
 * Time: 4:34:17 PM
 */
public class Authenticator {

    private javax.crypto.Mac mac = null;
    private String apiKey = null;

    public Authenticator() {
    }

    public Authenticator(String apiKey, String secret) throws Exception {
        this.setSecret(secret);
        this.setApiKey(apiKey);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Indicates if this {@code SignTool} is prepared to create signatures.
     *
     * @return {@code TRUE} if it is prepared otherwise {@code FALSE}
     */
    public boolean prepared() {
        return !(mac == null);
    }

    /**
     * This method converts SecretKey into crypto instance.
     *
     * @param secret SecretKey
     * @throws Exception If an error occurs creating the crypto.
     */
    public void setSecret(String secret) throws Exception {
        if (secret == null) {
            mac = null;
        } else {
            mac = Mac.getInstance("HmacSHA1");
            byte[] keyBytes = secret.getBytes("UTF8");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
        }
    }

    /**
     * Creates a signature for a given String.
     * @param data The String to create the signature for.
     * @return The signature.
     * @throws Exception If an error occurs creating the signature.
     */
    public String sign(String data) throws Exception {
        if (mac == null) {
            return null;
        }

        // Signed String mst be a BASE64 encoded.
        return encodeBase64(mac.doFinal(data.getBytes("UTF8"))).trim();
    }


    /**
     * Base 64 encode a byte array.
     *
     * @param data The bytes to be encoded.
     * @return The base64 encoded String.
     */
    protected String encodeBase64(byte[] data) {
        String base64 = new sun.misc.BASE64Encoder().encodeBuffer(data);
        if (base64.endsWith("\r\n")) base64 = base64.substring(0, base64.length() - 2);
        return base64;
    }

}
