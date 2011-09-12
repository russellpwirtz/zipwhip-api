package com.zipwhip.api.settings;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/1/11
 * Time: 11:02 AM
 */
public class PreferencesSettingsStoreTest {

    SettingsStore store;

    @Before
    public void setUp() throws Exception {
        store = new PreferencesSettingsStore();
    }

    @After
    public void tearDown() throws Exception {
        store.clear();
    }

    @Test
    public void testPutGetSessionKey() throws Exception {

        store.put(SettingsStore.Keys.SESSION_KEY, "1234-1234-5678-5678:123456");

        String sessionId = store.get(SettingsStore.Keys.SESSION_KEY);

        Assert.assertNotNull(sessionId);
        Assert.assertEquals("1234-1234-5678-5678:123456", sessionId);

        store.put(SettingsStore.Keys.SESSION_KEY, "4321-4321-9876-9876:9876543");
        sessionId = store.get(SettingsStore.Keys.SESSION_KEY);

        Assert.assertEquals("4321-4321-9876-9876:9876543", sessionId);
    }

    @Test
    public void testPutGetClientId() throws Exception {

        store.put(SettingsStore.Keys.CLIENT_ID, "1234-1234-5678-5678");

        String clientId = store.get(SettingsStore.Keys.CLIENT_ID);

        Assert.assertNotNull(clientId);
        Assert.assertEquals("1234-1234-5678-5678", clientId);

        store.put(SettingsStore.Keys.CLIENT_ID, "4321-4321-9876-9876");
        clientId = store.get(SettingsStore.Keys.CLIENT_ID);

        Assert.assertEquals("4321-4321-9876-9876", clientId);
    }

    @Test
    public void testClear() throws Exception {

        store.put(SettingsStore.Keys.SESSION_KEY, "1234-1234-5678-5678:123456");
        String sessionId = store.get(SettingsStore.Keys.SESSION_KEY);
        Assert.assertNotNull(sessionId);
        Assert.assertEquals("1234-1234-5678-5678:123456", sessionId);

        store.put(SettingsStore.Keys.CLIENT_ID, "1234-1234-5678-5678");
        String clientId = store.get(SettingsStore.Keys.CLIENT_ID);
        Assert.assertNotNull(clientId);
        Assert.assertEquals("1234-1234-5678-5678", clientId);

        store.remove(SettingsStore.Keys.SESSION_KEY);
        store.remove(SettingsStore.Keys.CLIENT_ID);

        Assert.assertEquals("", store.get(SettingsStore.Keys.SESSION_KEY));
        Assert.assertEquals("", store.get(SettingsStore.Keys.CLIENT_ID));
    }

    @Test
    public void testClearAll() throws Exception {

        store.put(SettingsStore.Keys.SESSION_KEY, "1234-1234-5678-5678:123456");
        String sessionId = store.get(SettingsStore.Keys.SESSION_KEY);
        Assert.assertNotNull(sessionId);
        Assert.assertEquals("1234-1234-5678-5678:123456", sessionId);

        store.put(SettingsStore.Keys.CLIENT_ID, "1234-1234-5678-5678");
        String clientId = store.get(SettingsStore.Keys.CLIENT_ID);
        Assert.assertNotNull(clientId);
        Assert.assertEquals("1234-1234-5678-5678", clientId);

        store.clear();

        Assert.assertEquals("", store.get(SettingsStore.Keys.SESSION_KEY));
        Assert.assertEquals("", store.get(SettingsStore.Keys.CLIENT_ID));
    }

    @Test
    public void testPutGetSessionKeyMemory() throws Exception {

        store = new MemorySettingStore();

        store.put(SettingsStore.Keys.SESSION_KEY, "1234-1234-5678-5678:123456");

        String sessionId = store.get(SettingsStore.Keys.SESSION_KEY);

        Assert.assertNotNull(sessionId);
        Assert.assertEquals("1234-1234-5678-5678:123456", sessionId);

        store.put(SettingsStore.Keys.SESSION_KEY, "4321-4321-9876-9876:9876543");
        sessionId = store.get(SettingsStore.Keys.SESSION_KEY);

        Assert.assertEquals("4321-4321-9876-9876:9876543", sessionId);
    }

    @Test
    public void testPutGetClientIdMemory() throws Exception {

        store = new MemorySettingStore();

        store.put(SettingsStore.Keys.CLIENT_ID, "1234-1234-5678-5678");

        String clientId = store.get(SettingsStore.Keys.CLIENT_ID);

        Assert.assertNotNull(clientId);
        Assert.assertEquals("1234-1234-5678-5678", clientId);

        store.put(SettingsStore.Keys.CLIENT_ID, "4321-4321-9876-9876");
        clientId = store.get(SettingsStore.Keys.CLIENT_ID);

        Assert.assertEquals("4321-4321-9876-9876", clientId);
    }

    @Test
    public void testClearMemory() throws Exception {

        store = new MemorySettingStore();

        store.put(SettingsStore.Keys.SESSION_KEY, "1234-1234-5678-5678:123456");
        String sessionId = store.get(SettingsStore.Keys.SESSION_KEY);
        Assert.assertNotNull(sessionId);
        Assert.assertEquals("1234-1234-5678-5678:123456", sessionId);

        store.put(SettingsStore.Keys.CLIENT_ID, "1234-1234-5678-5678");
        String clientId = store.get(SettingsStore.Keys.CLIENT_ID);
        Assert.assertNotNull(clientId);
        Assert.assertEquals("1234-1234-5678-5678", clientId);

        store.remove(SettingsStore.Keys.SESSION_KEY);
        store.remove(SettingsStore.Keys.CLIENT_ID);

        Assert.assertEquals("", store.get(SettingsStore.Keys.SESSION_KEY));
        Assert.assertEquals("", store.get(SettingsStore.Keys.CLIENT_ID));
    }

    @Test
    public void testClearAllMemory() throws Exception {

        store = new MemorySettingStore();

        store.put(SettingsStore.Keys.SESSION_KEY, "1234-1234-5678-5678:123456");
        String sessionId = store.get(SettingsStore.Keys.SESSION_KEY);
        Assert.assertNotNull(sessionId);
        Assert.assertEquals("1234-1234-5678-5678:123456", sessionId);

        store.put(SettingsStore.Keys.CLIENT_ID, "1234-1234-5678-5678");
        String clientId = store.get(SettingsStore.Keys.CLIENT_ID);
        Assert.assertNotNull(clientId);
        Assert.assertEquals("1234-1234-5678-5678", clientId);

        store.clear();

        Assert.assertEquals("", store.get(SettingsStore.Keys.SESSION_KEY));
        Assert.assertEquals("", store.get(SettingsStore.Keys.CLIENT_ID));
    }

}
