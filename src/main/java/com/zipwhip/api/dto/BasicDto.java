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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BasicDto");
        sb.append("{dateCreated=").append(dateCreated);
        sb.append(", lastUpdated=").append(lastUpdated);
        sb.append(", version=").append(version);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BasicDto basicDto = (BasicDto) o;

        if (version != basicDto.version) return false;
        if (dateCreated != null ? !dateCreated.equals(basicDto.dateCreated) : basicDto.dateCreated != null) return false;
        if (lastUpdated != null ? !lastUpdated.equals(basicDto.lastUpdated) : basicDto.lastUpdated != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dateCreated != null ? dateCreated.hashCode() : 0;
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        result = 31 * result + (int) (version ^ (version >>> 32));
        return result;
    }

}
