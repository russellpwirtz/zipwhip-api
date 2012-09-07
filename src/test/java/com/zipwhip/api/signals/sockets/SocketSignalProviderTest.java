package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.concurrent.TestUtil;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.SimpleExecutor;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import com.zipwhip.util.Factory;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA. User: jed Date: 8/30/11 Time: 3:30 PM
 */
public class SocketSignalProviderTest {

    private static final Logger LOG = Logger.getLogger(SocketSignalProviderTest.class);

    private SignalProvider provider;
    private MockSignalConnection signalConnection;
    private Presence presence;

    @Before
    public void setUp() throws Exception {

        signalConnection = new MockSignalConnection();
        provider = new SocketSignalProvider(signalConnection, null, null);

        presence = new Presence();
        presence.setCategory(PresenceCategory.Car);
    }

    @Test
    public void testDisconnectConnectDisconnect() throws Exception {

        ObservableFuture<ConnectionHandle> connectFuture1 = provider.connect();
        connectFuture1.await();
        assertNotNull(connectFuture1.get());
        assertTrue(connectFuture1.isSuccess());

        ObservableFuture<ConnectionHandle> disconnectFuture1 = provider.disconnect();
        disconnectFuture1.await();
        assertTrue(disconnectFuture1.isSuccess());

        ObservableFuture<ConnectionHandle> connectFuture2 = provider.connect();
        assertFalse(connectFuture1 == connectFuture2);
        connectFuture2.await();
        assertNotNull(connectFuture2.get());
        assertTrue(connectFuture2.isSuccess());

        ObservableFuture<ConnectionHandle> disconnectFuture2 = provider.disconnect();
        assertFalse(disconnectFuture1 == disconnectFuture2);
        disconnectFuture2.await();
        assertTrue(disconnectFuture2.isSuccess());
    }

    @Test
    public void testIsConnected() throws Exception {
        assertFalse(provider.getConnectionState() == ConnectionState.CONNECTED);
    }

    @Test
    public void testConnect() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        provider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testConnect - provider.onConnectionChanged " + item);
                assertTrue(item);
                latch.countDown();
            }
        });

        connect();

        assertTrue(latch.await(50, TimeUnit.SECONDS));
    }

    @Test
    public void testDisconnect() throws Exception {

        //synchronously connect
        connect();

        assertTrue("Not connected even though we just connected!", provider.getConnectionState() == ConnectionState.AUTHENTICATED);
        assertTrue(signalConnection.getConnectionState() == ConnectionState.CONNECTED);

        final CountDownLatch latch = new CountDownLatch(1);
        //add our handler
        provider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                LOG.debug("testDisconnect - provider.onConnectionChanged " + item);
                assertFalse(item);
                latch.countDown();
            }
        });

        // do a disconnect.
        Future<ConnectionHandle> task = provider.disconnect();
        task.get();

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertFalse(provider.getConnectionState() == ConnectionState.CONNECTED);
        assertFalse("Connection should be disconnected too!", signalConnection.getConnectionState() == ConnectionState.CONNECTED);
    }

    private void connect() throws Exception, InterruptedException, ExecutionException {
        Future<ConnectionHandle> future = provider.connect(null, null, presence);

        assertNotNull(future);
        assertNotNull(future.get());
    }

    @Test
    public void testOnSignalReceived() throws Exception {

        provider.connect().await();

        SignalObserver signalObserver = new SignalObserver();
        provider.getSignalReceivedEvent().addObserver(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.getSignalCommandReceivedEvent().addObserver(signalCommandObserver);

        signalConnection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 1l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        signalConnection.mockReceive(signalCommand);

        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
    }

    @Test
    public void testOnSignalReceivedOutOfOrder() throws Exception {

        provider.connect().await();

        SignalObserver signalObserver = new SignalObserver();
        provider.getSignalReceivedEvent().addObserver(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.getSignalCommandReceivedEvent().addObserver(signalCommandObserver);

        signalConnection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 1l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        signalConnection.mockReceive(signalCommand);

        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        signal = new Signal();
        signalCommand = new SignalCommand(signal);
        versionMapEntry = new VersionMapEntry("key", 3l);
        signalCommand.setVersion(versionMapEntry);
        signalObserver.signalReceived = false;
        signalCommandObserver.signalCommandReceived = false;
        signalConnection.mockReceive(signalCommand);
        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        signal = new Signal();
        signalCommand = new SignalCommand(signal);
        versionMapEntry = new VersionMapEntry("key", 2l);
        signalCommand.setVersion(versionMapEntry);
        signalObserver.signalReceived = false;
        signalCommandObserver.signalCommandReceived = false;
        signalConnection.mockReceive(signalCommand);
        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
        assertEquals(3, signalObserver.signalReceivedCount);
        assertEquals(3, signalCommandObserver.signalCommandReceivedCount);
    }

    @Test
    public void testOnSignalReceivedHoleTimeout() throws Exception {

        provider.connect().await();

        SignalObserver signalObserver = new SignalObserver();
        provider.getSignalReceivedEvent().addObserver(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.getSignalCommandReceivedEvent().addObserver(signalCommandObserver);

        VersionChangedObserver versionChangedObserver = new VersionChangedObserver();
        provider.getVersionChangedEvent().addObserver(versionChangedObserver);

        signalConnection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 1l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        signalConnection.mockReceive(signalCommand);

        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        signal = new Signal();
        signalCommand = new SignalCommand(signal);
        versionMapEntry = new VersionMapEntry("key", 3l);
        signalCommand.setVersion(versionMapEntry);
        signalObserver.signalReceived = false;
        signalCommandObserver.signalCommandReceived = false;
        signalConnection.mockReceive(signalCommand);
        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        signal = new Signal();
        signalCommand = new SignalCommand(signal);
        versionMapEntry = new VersionMapEntry("key", 4l);
        signalCommand.setVersion(versionMapEntry);
        signalObserver.signalReceived = false;
        signalCommandObserver.signalCommandReceived = false;
        signalConnection.mockReceive(signalCommand);
        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        Thread.sleep(2000); // Wait more than 1.5 seconds so that we will stop trying to fill the hole
        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
        assertEquals(3, signalObserver.signalReceivedCount);
        assertEquals(3, signalCommandObserver.signalCommandReceivedCount);

        assertEquals(3, versionChangedObserver.versions.size());
        assertEquals(1l, versionChangedObserver.versions.get(0));
        assertEquals(3l, versionChangedObserver.versions.get(1));
        assertEquals(4l, versionChangedObserver.versions.get(2));
    }

    @Test
    public void testOnMuchHigherVersionReceived() throws Exception {

        provider.connect().await();

        SignalObserver signalObserver = new SignalObserver();
        provider.getSignalReceivedEvent().addObserver(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.getSignalCommandReceivedEvent().addObserver(signalCommandObserver);

        VersionChangedObserver versionChangedObserver = new VersionChangedObserver();
        provider.getVersionChangedEvent().addObserver(versionChangedObserver);

        signalConnection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 200001l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        signalConnection.mockReceive(signalCommand);

        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        signal = new Signal();
        signalCommand = new SignalCommand(signal);
        versionMapEntry = new VersionMapEntry("key", 300000l);
        signalCommand.setVersion(versionMapEntry);
        signalObserver.signalReceived = false;
        signalCommandObserver.signalCommandReceived = false;
        signalConnection.mockReceive(signalCommand);
        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        Thread.sleep(2000); // Wait more than 1.5 seconds so that we will stop trying to fill the hole
        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
        assertEquals(2, signalObserver.signalReceivedCount);
        assertEquals(2, signalCommandObserver.signalCommandReceivedCount);

        assertEquals(2, versionChangedObserver.versions.size());
        assertEquals(200001l, versionChangedObserver.versions.get(0));
        assertEquals(300000l, versionChangedObserver.versions.get(1));
    }

    @Test
    public void testOnMuchHigherVersionReceivedAfterInit() throws Exception {

        provider.connect().await();

        Map<String, Long> versions = new HashMap<String, Long>();
        versions.put("key", 200001l);
        provider.setVersions(versions);
        SignalObserver signalObserver = new SignalObserver();
        provider.getSignalReceivedEvent().addObserver(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.getSignalCommandReceivedEvent().addObserver(signalCommandObserver);

        VersionChangedObserver versionChangedObserver = new VersionChangedObserver();
        provider.getVersionChangedEvent().addObserver(versionChangedObserver);

        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 300000l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        signalConnection.mockReceive(signalCommand);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());
        assertEquals(0, signalObserver.signalReceivedCount);
        assertEquals(0, signalCommandObserver.signalCommandReceivedCount);

        Thread.sleep(4000); // Wait more than 1.5 seconds so that we will stop trying to fill the hole
        assertEquals(1, signalObserver.signalReceivedCount);
        assertEquals(1, signalCommandObserver.signalCommandReceivedCount);

        assertEquals(1, versionChangedObserver.versions.size());
        assertEquals(300000l, versionChangedObserver.versions.get(0));
    }

    @Test
    public void testOnConnectionChanged() throws Exception {

        provider.getConnectionChangedEvent().addObserver(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testOnConnectionChanged - provider.onConnectionChanged CONNECT " + item);
            }
        });

        connect();

        Future<ConnectionHandle> task = provider.disconnect();
        task.get();
    }

    @Test
    public void testOnNewClientIdReceived() throws Exception {
        connect();

        String originalClientId = provider.getClientId();

        {
            Presence newPresence = provider.getPresence();
            assertNotNull("It seems that the API requires that this gets modified", newPresence.getAddress());
            assertEquals(originalClientId, newPresence.getAddress().getClientId());
        }

        // Simulate a reconnect so that we can witness the change
        {
            String newClientId = "4321-8765-4321-8765";
            signalConnection.mockReceive(new ConnectCommand(newClientId, null));

            assertNotSame(originalClientId, provider.getClientId());
            assertEquals(newClientId, provider.getClientId());

            Presence newPresence = provider.getPresence();
            assertEquals(newClientId, newPresence.getAddress().getClientId());
        }
    }

    @Test
    public void testOnSubscriptionComplete() throws Exception {
        connect();

        LOG.debug("Sending a SubscriptionCompleteCommand from the server to our client");

        signalConnection.getSent().clear();
        assertTrue(signalConnection.getSent().isEmpty());

        List<Object> channels = new ArrayList<Object>();
        SubscriptionCompleteCommand subscriptionCompleteCommand = new SubscriptionCompleteCommand(null, channels);
        signalConnection.mockReceive(subscriptionCompleteCommand);

        List<Command> sentCommand = signalConnection.getSent();
        assertEquals(Integer.valueOf(1), Integer.valueOf(sentCommand.size()));
        PresenceCommand presenceCommand = (PresenceCommand) sentCommand.get(0);
        assertNotNull(presenceCommand);
        assertEquals(Integer.valueOf(1), Integer.valueOf(presenceCommand.getPresence().size()));
        Presence newPresence = presenceCommand.getPresence().get(0);
        assertEquals(provider.getClientId(), newPresence.getAddress().getClientId());
    }

    @Test
    public void testOnPresenceReceived() throws Exception {
        connect();

        // In this section, we ensure that a null presence doesn't cause a crash
        // and that it ellicites a response with the client's presence object
        // and the correct client id!
        {
            LOG.debug("Sending a null presence command from the server to our client");

            signalConnection.getSent().clear();
            assertTrue(signalConnection.getSent().isEmpty());

            PresenceCommand presenceCommand = new PresenceCommand(null);
            signalConnection.mockReceive(presenceCommand);

            List<Command> sentCommand = signalConnection.getSent();
            assertEquals(Integer.valueOf(1), Integer.valueOf(sentCommand.size()));
            presenceCommand = (PresenceCommand) sentCommand.get(0);
            assertNotNull(presenceCommand);
            assertEquals(Integer.valueOf(1), Integer.valueOf(presenceCommand.getPresence().size()));
            Presence newPresence = presenceCommand.getPresence().get(0);
            assertEquals(provider.getClientId(), newPresence.getAddress().getClientId());
        }

        // In this section, we confirm verify that an empty list of Presence
        // objects results in the client resending their presence object
        {
            LOG.debug("Sending an empty presence command from the server to our client");

            signalConnection.getSent().clear();
            assertTrue(signalConnection.getSent().isEmpty());

            List<Presence> presenceList = new ArrayList<Presence>();
            PresenceCommand presenceCommand = new PresenceCommand(presenceList);
            signalConnection.mockReceive(presenceCommand);

            List<Command> sentCommand = signalConnection.getSent();
            assertEquals(Integer.valueOf(1), Integer.valueOf(sentCommand.size()));
            presenceCommand = (PresenceCommand) sentCommand.get(0);
            assertNotNull(presenceCommand);
            assertEquals(Integer.valueOf(1), Integer.valueOf(presenceCommand.getPresence().size()));
            Presence newPresence = presenceCommand.getPresence().get(0);
            assertEquals(provider.getClientId(), newPresence.getAddress().getClientId());
        }

        // In this section, we ensure that when a client receives a presence
        // list that does not obtain their client that they will respond with
        // their presence object
        {
            LOG.debug("Sending a presence command with other client's presence from the server to our client");

            signalConnection.getSent().clear();
            assertTrue(signalConnection.getSent().isEmpty());

            List<Presence> presenceList = new ArrayList<Presence>();
            Presence peerPresence = new Presence();
            peerPresence.setAddress(new ClientAddress("peerClientId"));
            presenceList.add(peerPresence);
            PresenceCommand presenceCommand = new PresenceCommand(presenceList);
            signalConnection.mockReceive(presenceCommand);

            List<Command> sentCommand = signalConnection.getSent();
            assertEquals(Integer.valueOf(1), Integer.valueOf(sentCommand.size()));
            presenceCommand = (PresenceCommand) sentCommand.get(0);
            assertNotNull(presenceCommand);
            assertEquals(Integer.valueOf(1), Integer.valueOf(presenceCommand.getPresence().size()));
            Presence newPresence = presenceCommand.getPresence().get(0);
            assertEquals(provider.getClientId(), newPresence.getAddress().getClientId());
        }

        // In this section, we communicate presence back to the client who see's
        // their object in the list and does not respond
        {
            LOG.debug("Sending a presence command with other client's presence from the server to our client");

            signalConnection.getSent().clear();
            assertTrue(signalConnection.getSent().isEmpty());

            List<Presence> presenceList = new ArrayList<Presence>();
            Presence peerPresence = new Presence();
            peerPresence.setAddress(new ClientAddress(provider.getClientId()));
            presenceList.add(peerPresence);
            PresenceCommand presenceCommand = new PresenceCommand(presenceList);
            signalConnection.mockReceive(presenceCommand);

            List<Command> sentCommand = signalConnection.getSent();
            assertEquals(Integer.valueOf(0), Integer.valueOf(sentCommand.size()));
        }
    }

    @Test
    public void testOnSignalVerificationReceived() throws Exception {
        connect();

        LOG.debug("Sending a SignalVerificationCommand from the server to our client");

        signalConnection.getSent().clear();
        assertTrue(signalConnection.getSent().isEmpty());

        SignalVerificationCommand signalVerificationCommand = new SignalVerificationCommand();
        signalConnection.mockReceive(signalVerificationCommand);

        List<Command> sentCommand = signalConnection.getSent();
        assertEquals(Integer.valueOf(0), Integer.valueOf(sentCommand.size()));
    }

    @Test
    public void testOnVersionChanged() throws Exception {

        connect();

        LOG.debug("Sending a command with different versions");

        Map<String, Long> versions = provider.getVersions();
        assertTrue(versions.isEmpty());

        // Test null
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = null;
            signalCommand.setVersion(versionMapEntry);

            signalConnection.getSent().clear();
            signalConnection.mockReceive(signalCommand);
            assertTrue(versions.isEmpty());
            assertTrue(signalConnection.getSent().isEmpty());
        }

        // Test key is < 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key1", Long.valueOf(-1));
            signalCommand.setVersion(versionMapEntry);

            signalConnection.getSent().clear();
            signalConnection.mockReceive(signalCommand);
            assertTrue(versions.isEmpty());
            assertTrue(signalConnection.getSent().isEmpty());
        }
        // Test key is = 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key1", Long.valueOf(0));
            signalCommand.setVersion(versionMapEntry);

            signalConnection.getSent().clear();
            signalConnection.mockReceive(signalCommand);
            assertTrue(signalConnection.getSent().isEmpty());
            assertEquals(Integer.valueOf(0), versions.size());
            assertNull(versions.get("key1"));
        }
        // Test key is > 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key1", Long.valueOf(1));
            signalCommand.setVersion(versionMapEntry);

            signalConnection.getSent().clear();
            signalConnection.mockReceive(signalCommand);
            assertTrue(signalConnection.getSent().isEmpty());
            assertEquals(Integer.valueOf(1), versions.size());
            assertEquals(Long.valueOf(1), versions.get("key1"));
        }

        // Test another key > 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key2", Long.valueOf(11));
            signalCommand.setVersion(versionMapEntry);

            signalConnection.getSent().clear();
            signalConnection.mockReceive(signalCommand);
            assertTrue(signalConnection.getSent().isEmpty());
            assertEquals(Integer.valueOf(2), versions.size());
            assertEquals(Long.valueOf(1), versions.get("key1"));
            assertEquals(Long.valueOf(11), versions.get("key2"));
        }

    }

    @Test
    public void testPingEvent() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);

        provider.getPingReceivedEvent().addObserver(new Observer<PingEvent>() {
            @Override
            public void notify(Object sender, PingEvent item) {
                latch.countDown();
            }
        });

        TestUtil.awaitAndAssertSuccess(provider.connect());

        signalConnection.mockPingPongCommand(PingPongCommand.getShortformInstance());

        assertTrue(latch.await(10, TimeUnit.SECONDS));

    }

    /**
     * Custom observer to register that a Signal Command was received
     *
     * @author jeremy
     */
    private class SignalCommandObserver implements Observer<List<SignalCommand>> {
        boolean signalCommandReceived = false;
        int signalCommandReceivedCount;

        @Override
        public void notify(Object sender, List<SignalCommand> item) {
            assertNotNull(item);
            assertTrue(item.get(0) instanceof SignalCommand);
            signalCommandReceived = true;
            signalCommandReceivedCount++;
            System.out.println("Received signal version " + item.get(0).getVersion().getValue());
        }

        /**
         * @return the signalCommandReceived
         */
        public final boolean isSignalCommandReceived() {
            return signalCommandReceived;
        }
    }

    /**
     * Custom observer to register that a Signal was received
     *
     * @author jeremy
     */
    private class SignalObserver implements Observer<List<com.zipwhip.api.signals.Signal>> {

        boolean signalReceived = false;
        int signalReceivedCount;

        @Override
        public void notify(Object sender, List<Signal> item) {
            System.out.println("testOnSignalReceived - provider.onSignalReceived " + item);
            assertNotNull(item);
            assertTrue(item.get(0).getClass().getSimpleName(), item.get(0) instanceof Signal);
            signalReceived = true;
            signalReceivedCount++;
        }

        /**
         * @return the signalReceived
         */
        public final boolean isSignalReceived() {
            return signalReceived;
        }

    }

    /**
     * Custom observer to register that a version changed
     *
     * @author jed
     */
    private class VersionChangedObserver implements Observer<VersionMapEntry> {
        int versionChangedCount;
        List<Long> versions = new ArrayList<Long>();

        @Override
        public void notify(Object sender, VersionMapEntry item) {
            System.out.println("testOnVersionChanged - provider.onSignalReceived " + item);
            assertNotNull(item);
            versionChangedCount++;
            versions.add(item.getValue());
        }

    }

}
