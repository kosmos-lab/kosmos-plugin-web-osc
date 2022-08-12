package de.kosmos_lab.platform.plugins.web.osc.data;

import de.sciss.net.OSCMessage;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSCChannelMuteListener implements OSCChannelListener {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCChannelMuteListener.class);

    private final OSCChannel channel;
    private final Pattern[] mutePattern;

    public OSCChannelMuteListener(OSCChannel channel, Pattern[] mutePattern) {
        this.channel = channel;
        this.mutePattern = mutePattern;
    }

    @Override
    public void messageReceived(OSCMessage oscMessage) {
        for (Pattern p : mutePattern) {
            //logger.info("trying to match {} to {}", p.pattern(), oscMessage.getName());
            Matcher m = p.matcher(oscMessage.getName());
            if (m.matches()) {
                if ((Integer) oscMessage.getArg(0) == 1) {
                    if (!channel.muted) {
                        return;
                    }
                    channel.muted = false;
                } else {
                    if (channel.muted) {
                        return;
                    }
                    channel.muted = true;
                }
                logger.info("Handling mute for {}  to {}", channel.name, channel.muted);
                return;
            }
        }
    }
}