package com.zipwhip.api;

import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 17, 2009
 * Time: 8:25:37 PM
 * <p/>
 * The Zipwhip SignalAddress scheme, broken into different parts. This is mostly for parsing and validation.
 */
public class Address {

    private String value;

    private String scheme;
    private String authority;
    private String query;

    public Address() {
    }

    public Address(String parsable) {
        this.parse(parsable);
    }

    public Address parse(String parsable) {

        if (parsable == null) return null;

        this.value = parsable;

        String[] parts = value.split("/");

        if (parts != null && parts.length > 1) {
            this.scheme = parts[0].replace(":", "");
            this.authority = parts[1];
        }

        if (parts != null && parts.length > 2) {
            this.query = parts[2];
        }

        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * From the parsed address build an address of the format device:/5555555555/0
     *
     * @return A device address of the format device:/5555555555/0
     */
    public String toDeviceAddress() {
        return StringUtil.join(StringUtil.exists(scheme) ? scheme : "device", ":/", StringUtil.exists(authority) ? authority : value, "/", StringUtil.exists(query) ? query : "0");
    }

}
