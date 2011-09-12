package com.zipwhip.api.settings;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 9/7/11
 * Time: 11:29 AM
 */
public class SettingsVersionStoreTest {

    VersionStore store;

    @Before
    public void setUp() throws Exception {
        store = new SettingsVersionStore(new PreferencesSettingsStore());
        store.clear();
    }

    @After
    public void tearDown() throws Exception {
        store.clear();
    }

    @Test
    public void testGetAll() throws Exception {

        Map<String, Long> memMap = new HashMap<String, Long>();
        memMap.put("a", 1L);
        memMap.put("b", 2L);

        for (String key : memMap.keySet()) {
            store.set(key, memMap.get(key));
        }

        Map<String, Long> vm = store.get();

        Assert.assertNotNull(vm);
        Assert.assertEquals(vm, memMap);

        store = new SettingsVersionStore(new PreferencesSettingsStore());

        Map<String, Long> mm = store.get();
        Assert.assertEquals(mm, memMap);
    }

    @Test
    public void testGet() throws Exception {

        Map<String, Long> memMap = new HashMap<String, Long>();
        memMap.put("c", 8L);
        memMap.put("d", 9L);

        for (String key : memMap.keySet()) {
            store.set(key, memMap.get(key));
        }

        Long vc = store.get("c");

        Assert.assertNotNull(vc);
        Assert.assertTrue(vc == 8L);

        Long vd = store.get("d");

        Assert.assertNotNull(vd);
        Assert.assertTrue(vd == 9L);
    }

    @Test
    public void testSet() throws Exception {

        Assert.assertTrue(store.set("e", 3L));
        Assert.assertFalse(store.set("e", 2L));

        Map<String, Long> memMap = new HashMap<String, Long>();
        memMap.put("e", 3L);

        Assert.assertEquals(store.get(), memMap);
    }

    @Test
    public void testClear() throws Exception {

        store.set("f", 4L);
        store.set("g", 5L);

        store.clear();

        Assert.assertTrue(store.get().isEmpty());
    }

}
