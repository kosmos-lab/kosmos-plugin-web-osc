package de.kosmos_lab.platform.plugins.web.osc.data;

import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;

import java.net.SocketAddress;
import java.util.HashSet;

public abstract class OSCChannel implements OSCListener {

    final String name;
    final OSCController controller;
    public HashSet<OSCChannelListener> listeners = new HashSet<>();
    boolean muted = false;
    String[] muteFormat;

    public OSCChannel(OSCController controller, String name) {
        this.name = name;
        this.controller = controller;
    }

    public String getName() {
        return this.name;
    }

    public boolean isMuted() {
        return this.muted;
    }

    public void addListener(OSCChannelListener listener) {
        this.listeners.add(listener);
    }

    public void messageReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
        for (OSCChannelListener listener : this.listeners) {
            listener.messageReceived(oscMessage);
        }
    }

    public boolean setMute(boolean muted) {

        for (String f : muteFormat) {
            controller.send(new OSCMessage(f, new Object[]{(muted) ? (0) : (1)}));
        }

        return true;

    }
}


