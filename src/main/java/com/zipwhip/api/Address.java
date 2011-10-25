package com.zipwhip.api;

import com.zipwhip.util.CollectionUtil;
import com.zipwhip.util.StringUtil;

/**
 * Created by IntelliJ IDEA.
 * Date: Jul 17, 2009
 * Time: 8:25:37 PM
 * <p/>
 * The Zipwhip SignalAddress scheme, broken into different parts. This is mostly for parsing and validation.
 */
public class Address {

    private String value; // the original value
    private String query;
    private String authority;
    private String scheme;

    public Address() {
    }

    public Address(String parsable) {
        this.setAddress(parsable);
    }

    public Address(String scheme, String authority) {
        this.scheme = scheme;
        this.authority = authority;
    }

    public Address(String scheme, String authority, String query) {
        this(scheme, authority);
        this.query = query;
    }

    public Address set(String scheme, String authority) {
        this.scheme = scheme;
        this.authority = authority;
        return this;
    }

    public Address set(String scheme, String authority, int query) {
        return this.set(scheme, authority, Integer.toString(query));
    }

    public Address set(String scheme, String authority, String query) {
        this.set(scheme, authority);
        this.query = query;
        return this;
    }

    public Address setAddress(String value) {

        if (StringUtil.isNullOrEmpty(value)) {
            return this;
        }

        this.value = value;

        String[] parts = value.split("/");

        if (CollectionUtil.isNullOrEmpty(parts)) {
            return null;
        }

        if (parts.length > 1) {
            this.scheme = parts[0].replace(":", "");
            this.authority = parts[1];
        }

        if (parts.length > 2) {
            this.query = parts[2];
        }

        return this;
    }

    public String toString() {

        String result = StringUtil.join(scheme, ":/", authority);

        if (!StringUtil.isNullOrEmpty(query)) {
            result += StringUtil.join("/", query);
        }

        return result;
    }

    public static String getQuery(String parsable) {
        Address result = new Address(parsable);
        return result.query;
    }

    public static String getAuthority(String parsable) {
        Address result = new Address(parsable);
        return result.authority;
    }

    public static Address decode(String parsable) {

        Address result = new Address(parsable);

        if (!StringUtil.isNullOrEmpty(result.authority)) {
            return result;
        }

        return null;
    }

    public static String encode(String scheme, String authority, String query) {
        return new Address(scheme, authority, query).toString();
    }

    public static String encode(String scheme, String authority) {
        return new Address(scheme, authority).toString();
    }

    public static String stripToMobileNumber(String address) {

        String mobileNumber;

        if (StringUtil.isNullOrEmpty(address)){
            return null;
        }

        if (address.contains("-0")) address = address.replace("-0", "");

        if (address.contains("/0")) address = address.replace("/0", "");

        if (address.contains(":")) {
            mobileNumber = Address.getAuthority(address);
        } else {
            mobileNumber = address;
        }
        return mobileNumber;
    }

    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (obj instanceof Address) {

            Address address = (Address) obj;

            if (!StringUtil.equals(address.authority, this.authority)) {
                return false;
            }
            if (!StringUtil.equals(address.scheme, this.scheme)) {
                return false;
            }
            if (!StringUtil.equals(address.query, this.query)) {
                return false;
            }

            return true;
        }
        return false;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

}
