package de.kosmos_lab.platform.plugins.web.osc;

import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class OSCClient extends Thread {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCClient.class);


    private final int port;
    private final String host;
    private final OSCListener listener;
    private long lastmessage;
    private de.sciss.net.OSCClient client;
    private boolean stopped = false;

    public OSCClient(OSCListener listener, String host, int port) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        this.lastmessage = 0;
    }

    public void stopClient() {

        this.stopped = true;

    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                logger.info("OSC Client connecting");

                this.client = de.sciss.net.OSCClient.newUsing(de.sciss.net.OSCClient.UDP);    // create UDP client with any free port number
                client.setTarget(new InetSocketAddress(this.host, this.port));
                client.addOSCListener(this.listener);
                client.dumpOutgoingOSC(1,System.err);
                client.dumpIncomingOSC(1,System.out);
                client.start();
                try {
                    client.send(new OSCMessage("/info"));
                    for ( int ch = 0;ch<32;ch++) {
                        //client.send(new OSCMessage(String.format("/ch/%02d/mix/fader"))

                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }
                while (client.isConnected()) {
                    //logger.info("client is still connected");
                    if (stopped) {
                        break;
                    }
                    if (System.currentTimeMillis() - lastmessage > 8000l) {
                        client.send(new OSCMessage("/xremote"));
                        lastmessage = System.currentTimeMillis();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                client.stop();
                client.dispose();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("OSC Client exited");

    }

    public void send(OSCMessage oscMessage) {
        try {
            long started = System.currentTimeMillis();
            while (this.client == null) {

                Thread.sleep(50);
                if (System.currentTimeMillis() - started > 5000) {
                    logger.warn("aborting send");
                    return;
                }
            }
            this.client.send(oscMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
