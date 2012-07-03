package com.zipwhip.api.dto;

/**
 * Created by IntelliJ IDEA.
 * User: Austin
 * Date: 6/30/11
 * Time: 3:52 PM
 */
public class CarbonEvent extends BasicDto {

    private static final long serialVersionUID = 5874121954952365L;

    public String carbonDescriptor;

    public String getCarbonDescriptor() {
        return carbonDescriptor;
    }

    public void setCarbonDescriptor(String carbonDescriptor) {
        this.carbonDescriptor = carbonDescriptor;
    }

    @Override
    public String toString() {
        return carbonDescriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CarbonEvent)) return false;
        if (!super.equals(o)) return false;

        CarbonEvent that = (CarbonEvent) o;

        if (!carbonDescriptor.equals(that.carbonDescriptor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + carbonDescriptor.hashCode();
        return result;
    }

}
