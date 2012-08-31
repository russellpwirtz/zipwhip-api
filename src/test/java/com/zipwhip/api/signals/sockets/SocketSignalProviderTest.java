package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.*;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.Observer;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA. User: jed Date: 8/30/11 Time: 3:30 PM
 */
public class SocketSignalProviderTest {

    private static final Logger LOG = Logger.getLogger(SocketSignalProviderTest.class);

    private SignalProvider provider;
    private MockSignalConnection connection;
    private Presence presence;

    @Before
    public void setUp() throws Exception {
        connection = new MockSignalConnection();
        provider = new SocketSignalProvider(connection);

        presence = new Presence();
        presence.setCategory(PresenceCategory.Car);

    }

    @Test
    public void testDisconnectConnectDisconnect() throws Exception{

        ObservableFuture<Boolean> connectFuture1 = provider.connect();
        connectFuture1.await();
        assertTrue(connectFuture1.get());
        assertTrue(connectFuture1.isSuccess());

        ObservableFuture<Void> disconnectFuture1 = provider.disconnect();
        disconnectFuture1.await();
        assertTrue(disconnectFuture1.isSuccess());

        ObservableFuture<Boolean> connectFuture2 = provider.connect();
        assertFalse(connectFuture1 == connectFuture2);
        connectFuture2.await();
        assertTrue(connectFuture2.get());
        assertTrue(connectFuture2.isSuccess());

        ObservableFuture<Void> disconnectFuture2 = provider.disconnect();
        assertFalse(disconnectFuture1 == disconnectFuture2);
        disconnectFuture2.await();
        assertTrue(disconnectFuture2.isSuccess());
    }

    @Test
    public void testIsConnected() throws Exception {
        assertFalse(provider.isConnected());
    }

    @Test
    public void testConnect() throws Exception {

        provider.onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testConnect - provider.onConnectionChanged " + item);
                assertTrue(item);
            }
        });

        connect();
    }

    @Test
    public void testDisconnect() throws Exception {

        connect();

        provider.onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testDisconnect - provider.onConnectionChanged " + item);
                assertFalse(item);
            }
        });

        Future<Void> task = provider.disconnect();
        task.get();
    }

    private void connect() throws Exception, InterruptedException, ExecutionException {
        Future<Boolean> future = provider.connect(null, null, presence);

        assertNotNull(future);
        assertTrue(future.get());
    }

    @Test
    public void testOnSignalReceived() throws Exception {

        Boolean signalReceived = false;
        Boolean signalCommandReceived = false;

        SignalObserver signalObserver = new SignalObserver();
        provider.onSignalReceived(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.onSignalCommandReceived(signalCommandObserver);

        connection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 1l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        connection.mockReceive(signalCommand);

        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
    }

    @Test
    public void testOnSignalReceivedOutOfOrder() throws Exception {

        SignalObserver signalObserver = new SignalObserver();
        provider.onSignalReceived(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.onSignalCommandReceived(signalCommandObserver);

        connection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 1l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        connection.mockReceive(signalCommand);

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
        connection.mockReceive(signalCommand);
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
        connection.mockReceive(signalCommand);
        assertTrue(signalObserver.isSignalReceived());
        assertTrue(signalCommandObserver.isSignalCommandReceived());
        assertEquals(3, signalObserver.signalReceivedCount);
        assertEquals(3, signalCommandObserver.signalCommandReceivedCount);
    }

    @Test
    public void testOnSignalReceivedHoleTimeout() throws Exception {

        SignalObserver signalObserver = new SignalObserver();
        provider.onSignalReceived(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.onSignalCommandReceived(signalCommandObserver);

        VersionChangedObserver versionChangedObserver = new VersionChangedObserver();
        provider.onVersionChanged(versionChangedObserver);

        connection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 1l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        connection.mockReceive(signalCommand);

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
        connection.mockReceive(signalCommand);
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
        connection.mockReceive(signalCommand);
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

        SignalObserver signalObserver = new SignalObserver();
        provider.onSignalReceived(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.onSignalCommandReceived(signalCommandObserver);

        VersionChangedObserver versionChangedObserver = new VersionChangedObserver();
        provider.onVersionChanged(versionChangedObserver);

        connection.send(null);
        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 200001l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        connection.mockReceive(signalCommand);

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
        connection.mockReceive(signalCommand);
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

        Map<String, Long> versions = new HashMap<String, Long>();
        versions.put("key", 200001l);
        provider.setVersions(versions);
        SignalObserver signalObserver = new SignalObserver();
        provider.onSignalReceived(signalObserver);

        SignalCommandObserver signalCommandObserver = new SignalCommandObserver();
        provider.onSignalCommandReceived(signalCommandObserver);

        VersionChangedObserver versionChangedObserver = new VersionChangedObserver();
        provider.onVersionChanged(versionChangedObserver);

        Signal signal = new Signal();
        SignalCommand signalCommand = new SignalCommand(signal);
        VersionMapEntry versionMapEntry = new VersionMapEntry("key", 300000l);
        signalCommand.setVersion(versionMapEntry);

        assertFalse(signalObserver.isSignalReceived());
        assertFalse(signalCommandObserver.isSignalCommandReceived());

        connection.mockReceive(signalCommand);

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

        provider.onConnectionChanged(new Observer<Boolean>() {
            @Override
            public void notify(Object sender, Boolean item) {
                System.out.println("testOnConnectionChanged - provider.onConnectionChanged CONNECT " + item);
            }
        });

        connect();

        Future<Void> task = provider.disconnect();
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
            connection.mockReceive(new ConnectCommand(newClientId, null));

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

        connection.getSent().clear();
        assertTrue(connection.getSent().isEmpty());

        List<Object> channels = new ArrayList<Object>();
        SubscriptionCompleteCommand subscriptionCompleteCommand = new SubscriptionCompleteCommand(null, channels);
        connection.mockReceive(subscriptionCompleteCommand);

        List<Command> sentCommand = connection.getSent();
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

            connection.getSent().clear();
            assertTrue(connection.getSent().isEmpty());

            PresenceCommand presenceCommand = new PresenceCommand(null);
            connection.mockReceive(presenceCommand);

            List<Command> sentCommand = connection.getSent();
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

            connection.getSent().clear();
            assertTrue(connection.getSent().isEmpty());

            List<Presence> presenceList = new ArrayList<Presence>();
            PresenceCommand presenceCommand = new PresenceCommand(presenceList);
            connection.mockReceive(presenceCommand);

            List<Command> sentCommand = connection.getSent();
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

            connection.getSent().clear();
            assertTrue(connection.getSent().isEmpty());

            List<Presence> presenceList = new ArrayList<Presence>();
            Presence peerPresence = new Presence();
            peerPresence.setAddress(new ClientAddress("peerClientId"));
            presenceList.add(peerPresence);
            PresenceCommand presenceCommand = new PresenceCommand(presenceList);
            connection.mockReceive(presenceCommand);

            List<Command> sentCommand = connection.getSent();
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

            connection.getSent().clear();
            assertTrue(connection.getSent().isEmpty());

            List<Presence> presenceList = new ArrayList<Presence>();
            Presence peerPresence = new Presence();
            peerPresence.setAddress(new ClientAddress(provider.getClientId()));
            presenceList.add(peerPresence);
            PresenceCommand presenceCommand = new PresenceCommand(presenceList);
            connection.mockReceive(presenceCommand);

            List<Command> sentCommand = connection.getSent();
            assertEquals(Integer.valueOf(0), Integer.valueOf(sentCommand.size()));
        }
    }

    @Test
    public void testOnSignalVerificationReceived() throws Exception {
        connect();

        LOG.debug("Sending a SignalVerificationCommand from the server to our client");

        connection.getSent().clear();
        assertTrue(connection.getSent().isEmpty());

        SignalVerificationCommand signalVerificationCommand = new SignalVerificationCommand();
        connection.mockReceive(signalVerificationCommand);

        List<Command> sentCommand = connection.getSent();
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

            connection.getSent().clear();
            connection.mockReceive(signalCommand);
            assertTrue(versions.isEmpty());
            assertTrue(connection.getSent().isEmpty());
        }

        // Test key is < 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key1", Long.valueOf(-1));
            signalCommand.setVersion(versionMapEntry);

            connection.getSent().clear();
            connection.mockReceive(signalCommand);
            assertTrue(versions.isEmpty());
            assertTrue(connection.getSent().isEmpty());
        }
        // Test key is = 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key1", Long.valueOf(0));
            signalCommand.setVersion(versionMapEntry);

            connection.getSent().clear();
            connection.mockReceive(signalCommand);
            assertTrue(connection.getSent().isEmpty());
            assertEquals(Integer.valueOf(0), versions.size());
            assertNull(versions.get("key1"));
        }
        // Test key is > 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key1", Long.valueOf(1));
            signalCommand.setVersion(versionMapEntry);

            connection.getSent().clear();
            connection.mockReceive(signalCommand);
            assertTrue(connection.getSent().isEmpty());
            assertEquals(Integer.valueOf(1), versions.size());
            assertEquals(Long.valueOf(1), versions.get("key1"));
        }

        // Test another key > 0
        {
            SignalCommand signalCommand = new SignalCommand(new Signal());
            VersionMapEntry versionMapEntry = new VersionMapEntry("key2", Long.valueOf(11));
            signalCommand.setVersion(versionMapEntry);

            connection.getSent().clear();
            connection.mockReceive(signalCommand);
            assertTrue(connection.getSent().isEmpty());
            assertEquals(Integer.valueOf(2), versions.size());
            assertEquals(Long.valueOf(1), versions.get("key1"));
            assertEquals(Long.valueOf(11), versions.get("key2"));
        }

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
