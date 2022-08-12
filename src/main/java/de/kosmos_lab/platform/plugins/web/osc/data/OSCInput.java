package de.kosmos_lab.platform.plugins.web.osc.data;

import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class OSCInput extends OSCChannel {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCInput.class);
    private final int[] channel;
    private boolean muted = false;


    public OSCInput(OSCController controller, String name, int[] channel) {
        super(controller, name);

        this.channel = channel;
        this.controller.addInput(this);
        Pattern[] mutePattern = new Pattern[channel.length];
        muteFormat = new String[channel.length];

        for (int i = 0; i < channel.length; i++) {
            muteFormat[i] = String.format("/ch/%02d/mix/on", channel[i]);
            mutePattern[i] = Pattern.compile("^" + muteFormat[i] + "$");
        }
        this.addListener(new OSCChannelMuteListener(this, mutePattern));


    }

    public int[] getChannel() {
        return this.channel;
    }


}
