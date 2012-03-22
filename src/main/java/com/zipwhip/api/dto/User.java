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

//    long id;
//    String address;
    String mobileNumber;
    String firstName;
    String lastName;
//    String phone;
    String email;
//    long deviceId;
    long moCount;
    long zoCount;
//    String latlong;
    String loc;
    String notes;
    String carrier;
    String zipcode;
    String phoneKey;

    long websiteDeviceId;
//    String thread;
//    String fwd;
//    String channel;
//    String city;
//    String state;




//    public String getFwd() {
//        return fwd;
//    }
//
//    public void setFwd(String fwd) {
//        this.fwd = fwd;
//    }
//
//    public String getCity() {
//        return city;
//    }
//
//    public void setCity(String city) {
//        this.city = city;
//    }
//
//    public String getState() {
//        return state;
//    }
//
//    public void setState(String state) {
//        this.state = state;
//    }
//
//    public long getDeviceId() {
//        return deviceId;
//    }
//
//    public void setDeviceId(long deviceId) {
//        this.deviceId = deviceId;
//    }

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

//    public String getLatlong() {
//        return latlong;
//    }
//
//    public void setLatlong(String latlong) {
//        this.latlong = latlong;
//    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

//    public String getChannel() {
//        return channel;
//    }
//
//    public void setChannel(String channel) {
//        this.channel = channel;
//    }

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

//    public String getThread() {
//        return thread;
//    }
//
//    public void setThread(String thread) {
//        this.thread = thread;
//    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

//    public String getAddress() {
//        return address;
//    }
//
//    public void setAddress(String address) {
//        this.address = address;
//    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

//    public long getId() {
//        return id;
//    }
//
//    public void setId(long id) {
//        this.id = id;
//    }

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

//    public String getPhone() {
//        return phone;
//    }
//
//    public void setPhone(String phone) {
//        this.phone = phone;
//    }

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
//        toStringBuilder.append("\nId: ").append(id);
//        toStringBuilder.append("\nAddress: ").append(address);
        toStringBuilder.append("\nMobile Number: ").append(mobileNumber);
        toStringBuilder.append("\nFirst Name: ").append(firstName);
        toStringBuilder.append("\nLast Name: ").append(lastName);
//        toStringBuilder.append("\nPhone: ").append(phone);
        toStringBuilder.append("\nDate Created: ").append(this.getDateCreated());
        toStringBuilder.append("\nLast Updated: ").append(this.getLastUpdated());
        toStringBuilder.append("\nEmail: ").append(email);
//        toStringBuilder.append("\nDeviceId: ").append(deviceId);
        toStringBuilder.append("\nMO COunt: ").append(moCount);
        toStringBuilder.append("\nZO Count: ").append(zoCount);
//        toStringBuilder.append("\nLatLong: ").append(latlong);
        toStringBuilder.append("\nLoc: ").append(loc);
        toStringBuilder.append("\nNotes: ").append(notes);
        toStringBuilder.append("\nCarrier: ").append(carrier);
        toStringBuilder.append("\nZipcode: ").append(zipcode);
        toStringBuilder.append("\nPhone Key: ").append(phoneKey);
        toStringBuilder.append("\nVersion: ").append(this.getVersion());
        toStringBuilder.append("\nWebsiteDeviceId: ").append(this.websiteDeviceId);
//        toStringBuilder.append("\nThread: ").append(thread);
//        toStringBuilder.append("\nFwd: ").append(fwd);
//        toStringBuilder.append("\nChannel: ").append(channel);
//        toStringBuilder.append("\nLast Updated: ").append(city);
//        toStringBuilder.append("\nLast Updated: ").append(state);

        return toStringBuilder.toString();
    }

}
