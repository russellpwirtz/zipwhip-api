package com.zipwhip.api.dto;

/**
 * User: Michael Date: Apr 26, 2010 Time: 6:35:06 PM
 * <p/>
 * A Contact is added by the user to a Device.
 * <p/>
 * The relationship is Contact[many] -> Device[one]
 */
public class User extends BasicDto {

    private static final long serialVersionUID = 5874891754952313L;

    String mobileNumber;
    String firstName;
    String lastName;
    String email;
    long moCount;
    long zoCount;
    String loc;
    String notes;
    String carrier;
    String zipcode;
    String phoneKey;
    long websiteDeviceId;

    public long getMoCount() {
        return moCount;
    }

    public void setMoCount(long moCount) {
        this.moCount = moCount;
    }

    public long getZoCount() {
        return zoCount;
    }

    public void setZoCount(long zoCount) {
        this.zoCount = zoCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getPhoneKey() {
        return phoneKey;
    }

    public void setPhoneKey(String phoneKey) {
        this.phoneKey = phoneKey;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public long getWebsiteDeviceId() {
        return websiteDeviceId;
    }

    public void setWebsiteDeviceId(long websiteDeviceId) {
        this.websiteDeviceId = websiteDeviceId;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder("==> Contact details:");
        toStringBuilder.append("\nMobile Number: ").append(mobileNumber);
        toStringBuilder.append("\nFirst Name: ").append(firstName);
        toStringBuilder.append("\nLast Name: ").append(lastName);
        toStringBuilder.append("\nDate Created: ").append(this.getDateCreated());
        toStringBuilder.append("\nLast Updated: ").append(this.getLastUpdated());
        toStringBuilder.append("\nEmail: ").append(email);
        toStringBuilder.append("\nMO COunt: ").append(moCount);
        toStringBuilder.append("\nZO Count: ").append(zoCount);
        toStringBuilder.append("\nLoc: ").append(loc);
        toStringBuilder.append("\nNotes: ").append(notes);
        toStringBuilder.append("\nCarrier: ").append(carrier);
        toStringBuilder.append("\nZipcode: ").append(zipcode);
        toStringBuilder.append("\nPhone Key: ").append(phoneKey);
        toStringBuilder.append("\nVersion: ").append(this.getVersion());
        toStringBuilder.append("\nWebsiteDeviceId: ").append(this.websiteDeviceId);

        return toStringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        if (!super.equals(o)) return false;

        User user = (User) o;

        if (moCount != user.moCount) return false;
        if (websiteDeviceId != user.websiteDeviceId) return false;
        if (zoCount != user.zoCount) return false;
        if (carrier != null ? !carrier.equals(user.carrier) : user.carrier != null) return false;
        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        if (firstName != null ? !firstName.equals(user.firstName) : user.firstName != null) return false;
        if (lastName != null ? !lastName.equals(user.lastName) : user.lastName != null) return false;
        if (loc != null ? !loc.equals(user.loc) : user.loc != null) return false;
        if (mobileNumber != null ? !mobileNumber.equals(user.mobileNumber) : user.mobileNumber != null) return false;
        if (notes != null ? !notes.equals(user.notes) : user.notes != null) return false;
        if (phoneKey != null ? !phoneKey.equals(user.phoneKey) : user.phoneKey != null) return false;
        if (zipcode != null ? !zipcode.equals(user.zipcode) : user.zipcode != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mobileNumber != null ? mobileNumber.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (int) (moCount ^ (moCount >>> 32));
        result = 31 * result + (int) (zoCount ^ (zoCount >>> 32));
        result = 31 * result + (loc != null ? loc.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + (carrier != null ? carrier.hashCode() : 0);
        result = 31 * result + (zipcode != null ? zipcode.hashCode() : 0);
        result = 31 * result + (phoneKey != null ? phoneKey.hashCode() : 0);
        result = 31 * result + (int) (websiteDeviceId ^ (websiteDeviceId >>> 32));
        return result;
    }
}
