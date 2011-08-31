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
 * Date: 8/31/11
 * Time: 4:14 PM
 */
public class PreferencesVersionManagerTest {

    PreferencesVersionManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new PreferencesVersionManager();
    }

    @After
    public void tearDown() throws Exception {
        manager.clearVersions();
    }

    @Test
    public void testGetVersions() throws Exception {

        Map<String, Long> memMap = new HashMap<String, Long>();
        memMap.put("a", 1L);
        memMap.put("b", 2L);

        for (String key : memMap.keySet()) {
            manager.setVersion(key, memMap.get(key));
        }

        Map<String, Long> vm = manager.getVersions();

        Assert.assertNotNull(vm);
        Assert.assertEquals(vm, memMap);
    }

    @Test
    public void testSetVersion() throws Exception {

        Assert.assertTrue(manager.setVersion("c", 3L));
        Assert.assertFalse(manager.setVersion("c", 2L));

        Map<String, Long> memMap = new HashMap<String, Long>();
        memMap.put("c", 3L);

        Assert.assertEquals(manager.getVersions(), memMap);
    }

    @Test
    public void testClearVersions() throws Exception {

        manager.setVersion("d", 4L);
        manager.setVersion("e", 5L);

        manager.clearVersions();

        Assert.assertTrue(manager.getVersions().isEmpty());
    }

}
