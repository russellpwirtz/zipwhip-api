package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.sockets.netty.ChannelState;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 8/27/12
 * Time: 3:52 PM
 */
public class StateManagerTest {

    StateManager<ChannelState> manager;

    @Before
    public void setUp() throws Exception {
        manager = new StateManager<ChannelState>();
    }

    @Test
    public void testAdd() throws Exception {
        assertNull(manager.get());
        manager.add(ChannelState.NONE, ChannelState.CONNECTED);
        assertNull(manager.get());
        manager.set(ChannelState.NONE);

        assertFalse(manager.transition(ChannelState.DISCONNECTING));
        assertTrue(manager.transition(ChannelState.CONNECTED));
    }

    @Test
    public void testSet() throws Exception {
        manager.set(ChannelState.NONE);
        assertEquals(ChannelState.NONE, manager.get());
    }

    @Test
    public void testTransition() throws Exception {
        manager.add(ChannelState.NONE, ChannelState.CONNECTED);
        manager.add(ChannelState.CONNECTED, ChannelState.DISCONNECTED);
        manager.set(ChannelState.NONE);
        assertEquals(ChannelState.NONE, manager.get());

        assertFalse(manager.transition(ChannelState.CONNECTING));
        assertEquals(ChannelState.NONE, manager.get());

        assertTrue(manager.transition(ChannelState.CONNECTED));
        assertEquals(ChannelState.CONNECTED, manager.get());
    }

    @Test
    public void testTransitionOrThrow() throws Exception {
        manager.add(ChannelState.NONE, ChannelState.CONNECTED);
        manager.add(ChannelState.CONNECTED, ChannelState.DISCONNECTED);
        manager.set(ChannelState.NONE);
        assertEquals(ChannelState.NONE, manager.get());

        manager.transitionOrThrow(ChannelState.CONNECTED);
        assertEquals(ChannelState.CONNECTED, manager.get());

        boolean threw = false;

        try {
            manager.transitionOrThrow(ChannelState.CONNECTING);
        } catch (Exception e) {
            threw = true;
            assertEquals(ChannelState.CONNECTED, manager.get());
        }

        assertTrue(threw);
    }

    @Test
    public void testEnsure() throws Exception {

        manager.set(ChannelState.DISCONNECTED);
        assertEquals(ChannelState.DISCONNECTED, manager.get());

        boolean threw = false;

        try {
            manager.ensure(ChannelState.NONE);
        } catch (Exception e) {
            threw = true;
            assertEquals(ChannelState.DISCONNECTED, manager.get());
        }

        assertTrue(threw);
    }

}
