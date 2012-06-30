package com.zipwhip.api.signals.sockets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.zipwhip.api.signals.Signal;
import com.zipwhip.api.signals.SignalProvider;
import com.zipwhip.api.signals.VersionMapEntry;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.PresenceCommand;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.api.signals.commands.SignalVerificationCommand;
import com.zipwhip.api.signals.commands.SubscriptionCompleteCommand;
import com.zipwhip.events.Observer;
import com.zipwhip.signals.address.ClientAddress;
import com.zipwhip.signals.presence.Presence;
import com.zipwhip.signals.presence.PresenceCategory;

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

        @Override
        public void notify(Object sender, List<SignalCommand> item) {
            assertNotNull(item);
            assertTrue(item.get(0) instanceof SignalCommand);
            signalCommandReceived = true;
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

        @Override
        public void notify(Object sender, List<Signal> item) {
            System.out.println("testOnSignalReceived - provider.onSignalReceived " + item);
            assertNotNull(item);
            assertTrue(item.get(0).getClass().getSimpleName(), item.get(0) instanceof Signal);
            signalReceived = true;
        }

        /**
         * @return the signalReceived
         */
        public final boolean isSignalReceived() {
            return signalReceived;
        }

    }
}
