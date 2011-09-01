package com.zipwhip.api.signals;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/1/11
 * Time: 11:02 AM
 */
public class DefaultSettingsStoreTest {

    SettingsStore store;

    @Before
    public void setUp() throws Exception {
        store = new DefaultSettingsStore();
    }

    @After
    public void tearDown() throws Exception {
        store.clearAll();
        store.clearVersions();
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

        store.clear(SettingsStore.Keys.SESSION_KEY);
        store.clear(SettingsStore.Keys.CLIENT_ID);

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

        store.clearAll();

        Assert.assertEquals("", store.get(SettingsStore.Keys.SESSION_KEY));
        Assert.assertEquals("", store.get(SettingsStore.Keys.CLIENT_ID));
    }

    @Test
    public void testGetVersions() throws Exception {

        Map<String, Long> memMap = new HashMap<String, Long>();
        memMap.put("a", 1L);
        memMap.put("b", 2L);

        for (String key : memMap.keySet()) {
            store.setVersion(key, memMap.get(key));
        }

        Map<String, Long> vm = store.getVersions();

        Assert.assertNotNull(vm);
        Assert.assertEquals(vm, memMap);
    }

    @Test
    public void testSetVersion() throws Exception {

        Assert.assertTrue(store.setVersion("c", 3L));
        Assert.assertFalse(store.setVersion("c", 2L));

        Map<String, Long> memMap = new HashMap<String, Long>();
        memMap.put("c", 3L);

        Assert.assertEquals(store.getVersions(), memMap);
    }

    @Test
    public void testClearVersions() throws Exception {

        store.setVersion("d", 4L);
        store.setVersion("e", 5L);

        store.clearVersions();

        Assert.assertTrue(store.getVersions().isEmpty());
    }

}
