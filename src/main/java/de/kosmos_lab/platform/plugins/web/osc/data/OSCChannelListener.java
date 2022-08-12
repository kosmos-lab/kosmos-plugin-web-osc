package de.kosmos_lab.platform.plugins.web.osc.data;

import de.sciss.net.OSCMessage;

public interface OSCChannelListener {
    void messageReceived(OSCMessage oscMessage);
}