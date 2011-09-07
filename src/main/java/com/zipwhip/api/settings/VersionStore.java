package com.zipwhip.api.settings;

import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: jed Date: 6/24/11 Time: 4:33 PM
 * <p/>
 * This class will record what versions have been seen before. Every signal has
 * a version. It is possible to have multiple versionKeys if you have more than
 * one subscription.
 * <p/>
 * In an Android environment, you will want to implement this via
 * SharedPreferences, or SQLite. In a Java application, you might want to use a
 * File or Preferences.
 */
public interface VersionStore {

    /**
     * Get all the current highest versions per key.
     * 
     * @return The current highest version number for each key or an empty list.
     * @throws Exception If a read exception occurs or the underlying data settingsStore is not available.
     */
    Map<String, Long> get() throws Exception;

    /**
     * Get the current highest version for versionKey.
     *
     * @param versionKey The version key to find a version for.
     * @return The current version number for versionKey or null if the key does not exist.
     * @throws Exception If a read exception occurs or the underlying data settingsStore is not available.
     */
    Long get(String versionKey) throws Exception;


    /**
     * Set the version if versionKey is new. Increment the version number if
     * newVersion is greater than the previous version.
     * 
     * @param versionKey The version key to update the version number on.
     * @param newVersion The version number to set for versionKey;
     * @return True if the version was set or incremented otherwise false.
     */
    boolean set(String versionKey, long newVersion);

    /**
     * Invalidate all keys and versions in the implemented persistence solution.
     */
    void clear();

}
