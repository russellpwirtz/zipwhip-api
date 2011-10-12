/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.zipwhip.api.subscriptions;

/**
 * @author Michael
 */
public class DotJoinSubscriptionEntry extends SubscriptionEntry {

    public DotJoinSubscriptionEntry(String targetDeviceAddress) {
        super("DotJoin", "{deviceAddress:'" + targetDeviceAddress + "'}");
    }

}
