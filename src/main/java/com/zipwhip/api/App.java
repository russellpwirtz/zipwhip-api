package com.zipwhip.api;

import com.zipwhip.api.signals.sockets.SocketSignalProvider;

/**
 * An example application to demonstrate connecting to Zipwhip and sending and
 * receiving messages.
 * 
 */
public class App {

    public static void main(String[] args) {
        startApp();
    }
    
    private static void startApp() {        
        ZipwhipClient client = new DefaultZipwhipClient(new HttpConnection(), new SocketSignalProvider());
    }

}
