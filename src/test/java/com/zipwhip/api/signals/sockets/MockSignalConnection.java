package com.zipwhip.api.signals.sockets;

import com.zipwhip.api.signals.JsonSignal;
import com.zipwhip.api.signals.PingEvent;
import com.zipwhip.api.signals.reconnect.ReconnectStrategy;
import com.zipwhip.api.signals.SignalConnection;
import com.zipwhip.api.signals.commands.Command;
import com.zipwhip.api.signals.commands.ConnectCommand;
import com.zipwhip.api.signals.commands.SerializingCommand;
import com.zipwhip.api.signals.commands.SignalCommand;
import com.zipwhip.events.Observer;
import com.zipwhip.lifecycle.DestroyableBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/30/11
 * Time: 3:33 PM
 */
public class MockSignalConnection extends DestroyableBase implements SignalConnection {

    private ExecutorService executor;

    // We need these to block for testing
    private List<Observer<Command>> receiveEvent = new ArrayList<Observer<Command>>();
    private List<Observer<Boolean>> connectEvent = new ArrayList<Observer<Boolean>>();
    private List<Observer<Boolean>> disconnectEvent = new ArrayList<Observer<Boolean>>();

    private boolean isConnected = false;

    private static final String SIGNAL_JSON = "{\"versionKey\":\"subscription__version_{class:ChannelAddress,channel:/device/5211ae17-d07f-465a-9cb4-0982d3c91952}\",\"action\":\"SIGNAL\",\"signal\":{\"content\":{\"to\":\"\",\"body\":\"Yo\",\"bodySize\":2,\"visible\":true,\"transmissionState\":{\"name\":\"QUEUED\",\"enumType\":\"com.zipwhip.outgoing.TransmissionState\"},\"type\":\"ZO\",\"metaDataId\":1040324202,\"dtoParentId\":106228502,\"scheduledDate\":null,\"thread\":\"\",\"carrier\":\"Tmo\",\"deviceId\":106228502,\"openMarketMessageId\":\"362c52b8-87ab-4e85-bbb5-f7a725ea0d7c\",\"lastName\":\"\",\"messageConsoleLog\":\"\",\"loc\":\"\",\"lastUpdated\":\"2011-08-25T12:02:41-07:00\",\"isParent\":false,\"class\":\"com.zipwhip.website.data.dto.Message\",\"deleted\":false,\"contactId\":268755902,\"isInFinalState\":false,\"uuid\":\"ce913542-93aa-421e-878a-5e9bad2b3ae6\",\"cc\":\"\",\"statusDesc\":\"\",\"subject\":\"\",\"encoded\":true,\"expectDeliveryReceipt\":false,\"transferedToCarrierReceipt\":null,\"version\":1,\"statusCode\":1,\"id\":13555722602,\"fingerprint\":\"2216445311\",\"parentId\":0,\"phoneKey\":\"\",\"smartForwarded\":false,\"fromName\":\"\",\"isSelf\":false,\"firstName\":\"\",\"sourceAddress\":\"4252466003\",\"deliveryReceipt\":null,\"dishedToOpenMarket\":null,\"errorState\":false,\"creatorId\":209644102,\"advertisement\":\"\\n\\nSent via T-Mobile Messaging\",\"bcc\":\"\",\"fwd\":\"\",\"contactDeviceId\":106228502,\"smartForwardingCandidate\":false,\"destAddress\":\"2069308934\",\"latlong\":\"\",\"DCSId\":\"\",\"new\":false,\"address\":\"ptn:/2069308934\",\"dateCreated\":\"2011-08-25T12:02:41-07:00\",\"UDH\":\"\",\"carbonedMessageId\":-1,\"mobileNumber\":\"2069308934\",\"channel\":\"\",\"isRead\":true},\"id\":\"13555722602\",\"scope\":\"device\",\"reason\":null,\"event\":\"send\",\"tag\":null,\"class\":\"com.zipwhip.signals.Signal\",\"uuid\":\"5211ae17-d07f-465a-9cb4-0982d3c91952\",\"type\":\"message\",\"uri\":\"/signal/message/send\"},\"channel\":\"/device/5211ae17-d07f-465a-9cb4-0982d3c91952\",\"version\":6}";

    @Override
    public synchronized Future<Boolean> connect() throws Exception {

        FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                isConnected = true;

                for (Observer<Boolean> o : connectEvent) {
                    o.notify(this, isConnected);
                }

                for (Observer<Command> o : receiveEvent) {
                    o.notify(this, new ConnectCommand("1234-5678-1234-5678", null, null));
                }

                return isConnected;
            }
        });

        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
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

                executor.shutdownNow();
                executor = null;

                for (Observer<Boolean> o : disconnectEvent) {
                    o.notify(this, requestReconnect);
                }

                return null;
            }
        });

        executor.execute(task);
        return task;
    }

    @Override
    public void send(SerializingCommand command) {

        for (Observer<Command> o : receiveEvent) {
            o.notify(this, new SignalCommand(new JsonSignal(SIGNAL_JSON)));
        }
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
        receiveEvent.add(observer);
    }

    @Override
    public void onConnect(Observer<Boolean> observer) {
        connectEvent.add(observer);
    }

    @Override
    public void onDisconnect(Observer<Boolean> observer) {
        disconnectEvent.add(observer);
    }

    @Override
    public void removeOnConnectObserver(Observer<Boolean> observer) {
        connectEvent.remove(observer);
    }

    @Override
    public void removeOnDisconnectObserver(Observer<Boolean> observer) {
        disconnectEvent.remove(observer);
    }

    @Override
    public void onPingEvent(Observer<PingEvent> observer) {
    }

    @Override
    public void startKeepalives() {
    }

    @Override
    public void stopKeepalives() {
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
    public int getPingTimeout() {
        return 0;
    }

    @Override
    public void setPingTimeout(int pingTimeout) {
    }

    @Override
    public int getPongTimeout() {
        return 0;
    }

    @Override
    public void setPongTimeout(int pongTimeout) {
    }

    @Override
    public ReconnectStrategy getReconnectStrategy() {
        return null;
    }

    @Override
    public void setReconnectStrategy(ReconnectStrategy strategy) {
    }

    @Override
    protected void onDestroy() {

        if (this.isConnected()) {
            this.disconnect();
        }
    }

}
