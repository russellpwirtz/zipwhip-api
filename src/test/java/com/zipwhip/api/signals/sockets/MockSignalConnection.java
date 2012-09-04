package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.sockets.netty.SignalConnectionBase;
import com.zipwhip.concurrent.FutureUtil;
import com.zipwhip.concurrent.ObservableFuture;
import com.zipwhip.events.ObservableHelper;
import com.zipwhip.events.Observer;
import com.zipwhip.executors.DebuggingExecutor;
import com.zipwhip.executors.FakeFuture;
import com.zipwhip.executors.SimpleExecutor;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 3:33 PM
 */
public class MockSignalConnection extends SignalConnectionBase implements SignalConnection {

    private static final Logger LOG = Logger.getLogger(MockSignalConnection.class);

//    protected Executor executor = new DebuggingExecutor(Executors.newSingleThreadExecutor(new NamedThreadFactory("MockSignalConnection-"))) {
//        @Override
//        public String toString() {
//            return "MockSignalConnectionExecutor";
//        }
//    };

    protected Executor executor = new DebuggingExecutor(SimpleExecutor.getInstance()) {
        @Override
        public String toString() {
            return "MockSignalConnectionExecutor";
        }
    };

    // We need these to block for testing
    protected final ObservableHelper<Command> receiveEvent = new ObservableHelper<Command>(executor);
    protected final ObservableHelper<Boolean> connectEvent = new ObservableHelper<Boolean>(executor);
    protected final ObservableHelper<Boolean> disconnectEvent = new ObservableHelper<Boolean>(executor);

    protected final List<Command> sent = new ArrayList<Command>();

    protected boolean isConnected = false;

    private static final String SIGNAL_JSON = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"to\":\"\",\"body\":\"Yo\",\"bodySize\":2,\"visible\":true,\"transmissionState\":{\"name\":\"QUEUED\",\"enumType\":\"com.zipwhip.outgoing.TransmissionState\"},\"type\":\"ZO\",\"metaDataId\":1040324202,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"362c52b8-87ab-4e85-bbb5-f7a725ea0d7c\",\"lastName\":\"\",\"messageConsoleLog\":\"\",\"loc\":\"\",\"lastUpdated\":\"2011-08-25T12:02:41-07:00\",\"isParent\":false,\"class\":\"com.zipwhip.website.data.dto.Message\",\"deleted\":false,\"contactId\":268755902,\"isInFinalState\":false,\"uuid\":\"ce913542-93aa-421e-878a-5e9bad2b3ae6\",\"cc\":\"\",\"statusDesc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,\"statusCode\":1,\"id\":13555722602,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"4252466003\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":209644102,\"advertisement\":\"\\n\\nSent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"latlong\":\"\",\"DCSId\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-25T12:02:41-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"id\":\"13555722602\",\"scope\":\"device\",\"reason\":null,\"event\":\"send\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/send\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":6}";

    public MockSignalConnection() {
        super(null);
    }

    @Override
    public synchronized Future<Boolean> connect() throws Exception {

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                isConnected = true;

                connectEvent.notifyObservers(this, isConnected);

                return isConnected;
            }
        });

        executor.execute(task);

        return task;
    }

    @Override
    public synchronized Future<Void> disconnect() {
        return disconnect(false);
    }

    @Override
    public Future<Void> disconnect(final boolean requestReconnect) {

        FutureTask<Void> task = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                isConnected = false;
                disconnectEvent.notifyObservers(this, requestReconnect);

                return null;
            }
        });

        executor.execute(task);
        return task;
    }

    @Override
    public ObservableFuture<Boolean> send(SerializingCommand command) {
        LOG.debug("Request received to send to server " + command);
        sent.add(command);

        if (command instanceof ConnectCommand) {
            receiveEvent.notifyObservers(this, new ConnectCommand("1234-5678-1234-5678", null));
        }

        // This doesnt appear to be the right place to send to
        //for (Observer<Command> o : receiveEvent) {
        //    o.notify(this, new SignalCommand(new JsonSignal(SIGNAL_JSON)));
        //}
        return FutureUtil.execute(null, this, new FakeFuture<Boolean>(true));
    }

    @Override
    public void keepalive() {

    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onMessageReceived(Observer<Command> observer) {
        receiveEvent.addObserver(observer);
    }

    @Override
    public void onConnect(Observer<Boolean> observer) {
        connectEvent.addObserver(observer);
    }

    @Override
    public void onDisconnect(Observer<Boolean> observer) {
        disconnectEvent.addObserver(observer);
    }

    @Override
    public void removeOnConnectObserver(Observer<Boolean> observer) {
        connectEvent.removeObserver(observer);
    }

    @Override
    public void removeOnMessageReceivedObserver(Observer<Command> observer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeOnDisconnectObserver(Observer<Boolean> observer) {
        disconnectEvent.removeObserver(observer);
    }

    @Override
    public void onPingEvent(Observer<PingEvent> observer) {
    }

    @Override
    public void onExceptionCaught(Observer<String> observer) {
    }

    @Override
    public void setHost(String host) {
    }

    @Override
    public void setPort(int port) {
    }

    @Override
    public ReconnectStrategy getReconnectStrategy() {
        return null;
    }

    @Override
    public void setReconnectStrategy(ReconnectStrategy strategy) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    /**
     * Provide a means to simulate traffic coming in from the signal server
     *
     * @param command
     */
    public void mockReceive(Command<?> command) {
        LOG.debug("notify observers of " + command);

        receiveEvent.notifyObservers(this, command);
    }

    /**
     * @return the sent
     */
    public final List<Command> getSent() {
        return sent;
    }

}
