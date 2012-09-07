package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.PingPongCommand;
import com.zipwhip.api.signals.sockets.netty.SignalConnectionBase;
import com.zipwhip.api.signals.sockets.netty.SignalConnectionBaseConnectionHandleBase;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.executors.DebuggingExecutor;
import com.zipwhip.executors.FakeObservableFuture;
import com.zipwhip.executors.SimpleExecutor;
import org.apache.log4j.Logger;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 3:33 PM
 */
public class MockSignalConnection extends SignalConnectionBase {

    private static final Logger LOGGER = Logger.getLogger(MockSignalConnection.class);

    protected Executor executor = new DebuggingExecutor(SimpleExecutor.getInstance()) {
        @Override
        public String toString() {
            return "MockSignalConnectionExecutor";
        }
    };

    protected final List<Command> sent = new ArrayList<Command>();
    private static int id = 0;

    private static final String SIGNAL_JSON = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"to\":\"\",\"body\":\"Yo\",\"bodySize\":2,\"visible\":true,\"transmissionState\":{\"name\":\"QUEUED\",\"enumType\":\"com.zipwhip.outgoing.TransmissionState\"},\"type\":\"ZO\",\"metaDataId\":1040324202,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"362c52b8-87ab-4e85-bbb5-f7a725ea0d7c\",\"lastName\":\"\",\"messageConsoleLog\":\"\",\"loc\":\"\",\"lastUpdated\":\"2011-08-25T12:02:41-07:00\",\"isParent\":false,\"class\":\"com.zipwhip.website.data.dto.Message\",\"deleted\":false,\"contactId\":268755902,\"isInFinalState\":false,\"uuid\":\"ce913542-93aa-421e-878a-5e9bad2b3ae6\",\"cc\":\"\",\"statusDesc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,\"statusCode\":1,\"id\":13555722602,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"4252466003\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":209644102,\"advertisement\":\"\\n\\nSent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"latlong\":\"\",\"DCSId\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-25T12:02:41-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"id\":\"13555722602\",\"scope\":\"device\",\"reason\":null,\"event\":\"send\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/send\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":6}";

    public MockSignalConnection() {
        super(SimpleExecutor.getInstance());
    }

    public MockSignalConnection(Executor executor) {
        super(executor);
    }

    /**
     * Provide a means to simulate traffic coming in from the signal server
     *
     * @param command
     */
    public void mockReceive(Command<?> command) {
        LOGGER.debug("notify observers of " + command);

        receiveEvent.notifyObservers(getConnectionHandle(), command);
    }

    public void mockPingPongCommand(PingPongCommand command) {
        receivePong(getConnectionHandle(), command);
    }

    /**
     * @return the sent
     */
    public final List<Command> getSent() {
        return sent;
    }

    @Override
    protected void executeDisconnectDestroyConnection(ConnectionHandle connectionHandle, boolean causedByNetwork) {
        // this is the execution of the actual disconnect.
        ((MockConnectionHandle) connectionHandle).causedByNetwork = causedByNetwork;
        ((MockConnectionHandle) connectionHandle).destroy();
    }

    @Override
    protected ConnectionHandle executeConnectReturnConnection(SocketAddress address) throws Throwable {
        return new MockConnectionHandle();
    }

    @Override
    protected Executor getExecutorForConnection(ConnectionHandle connectionHandle) {
        return executor;
    }

    @Override
    protected ObservableFuture<Boolean> executeSend(final ConnectionHandle connectionHandle, final Object command) {
        sent.add((Command) command);

        if (command instanceof ConnectCommand) {
            // we need to send it back in
            getExecutorForConnection(connectionHandle).execute(new Runnable() {
                @Override
                public void run() {
                    receiveEvent.notifyObservers(connectionHandle, new ConnectCommand("123-123-123"));
                }
            });
        }

        return new FakeObservableFuture<Boolean>(connectionHandle, Boolean.TRUE);
    }


    private class MockConnectionHandle extends SignalConnectionBaseConnectionHandleBase {

        private MockConnectionHandle() {
            super(id++, MockSignalConnection.this);
        }

    }
}
