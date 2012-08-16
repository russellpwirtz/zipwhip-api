package com.zipwhip.api.dto;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/16/12
 * Time: 9:38 AM
 */
public class TinyUrl implements Serializable {

    private static final long serialVersionUID = 6498245318894574732L;

    private String url;
    private String key;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "TinyUrl{" +
                "url='" + url + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TinyUrl)) return false;

        TinyUrl tinyUrl = (TinyUrl) o;

        if (key != null ? !key.equals(tinyUrl.key) : tinyUrl.key != null) return false;
        if (url != null ? !url.equals(tinyUrl.url) : tinyUrl.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }

}
