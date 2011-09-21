package com.zipwhip.api.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: Michael Date: 7/6/11 Time: 2:10 PM
 * <p/>
 * A base class for most of our DTO's (data transfer objects).
 */
public class BasicDto implements Serializable {

    private Date dateCreated;
    private Date lastUpdated;
    private long version;

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

}
