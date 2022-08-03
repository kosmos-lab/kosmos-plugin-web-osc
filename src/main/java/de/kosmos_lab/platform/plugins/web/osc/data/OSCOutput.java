package de.kosmos_lab.platform.plugins.web.osc.data;

import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSCOutput implements OSCListener {
    public static final int MAX_CHANNELS = 32;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCOutput.class);

    final Pattern patternChannel = Pattern.compile("^/ch/(?<channel>\\d*)/mix/(?<aux>\\d*)/level$");
    //final Pattern patternChannel = Pattern.compile("^/ch/(?<channel>\\d*)/mix/fader$");
    private final OSCController controller;

    private final String name;
    private final Pattern[] levelPattern;
    private final Pattern[] mutePattern;
    private final String[] muteFormat;
    private final String[] levelFormat;
    private boolean muted = false;
    private int level = 0;
    private float[] levels = new float[MAX_CHANNELS];

    public OSCOutput(OSCController controller, String name, String[] levelFormat, String[] muteFormat) {
        this.controller = controller;
        this.name = name;
        this.levelFormat = levelFormat;
        this.muteFormat = muteFormat;

        this.levelPattern = new Pattern[levelFormat.length];
        this.mutePattern = new Pattern[muteFormat.length];
        for (int i = 0; i < levelFormat.length; i++) {
            levelPattern[i] = Pattern.compile("^" + levelFormat[i].replaceAll("%02d", "(\\\\d*)") + "$");
        }
        for (int i = 0; i < muteFormat.length; i++) {
            mutePattern[i] = Pattern.compile("^" + muteFormat[i].replaceAll("%02d", "(\\\\d*)") + "$");
        }
        controller.addOutput(this);
        for (int ch = 0; ch < MAX_CHANNELS; ch++) {
            for (String f : levelFormat) {
                controller.send(new OSCMessage(String.format(f, ch)));
            }

        }
        for (String f : muteFormat) {
            controller.send(new OSCMessage(f));
        }
    }

    void set(int channel, float value) {
        levels[channel] = value;
    }

    public boolean setLevel(int channel, float value) {
        if (Math.abs(value - levels[channel]) < 0.01) {
            return false;
        }
        for (String f : levelFormat) {
            controller.send(new OSCMessage(String.format(f, channel), new Object[]{value}));
        }

        return true;

    }

    public boolean setMute(boolean muted) {

        for (String f : muteFormat) {
            controller.send(new OSCMessage(f, new Object[]{(muted) ? (0) : (1)}));
        }

        return true;

    }


    @Override
    public void messageReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
        for (Pattern p : levelPattern) {
            //logger.info("trying to match {} to {}",p.pattern(),oscMessage.getName());
            Matcher m = p.matcher(oscMessage.getName());
            if (m.matches()) {
                logger.info("Handling set level for {} Channel {} to {}", name, m.group(1), (Float) oscMessage.getArg(0));

                levels[Integer.parseInt(m.group(1))] = (Float) oscMessage.getArg(0);

                return;


            }
        }
        for (Pattern p : mutePattern) {
            //logger.info("trying to match {} to {}",p.pattern(),oscMessage.getName());
            Matcher m = p.matcher(oscMessage.getName());
            if (m.matches()) {

                if ((Integer) oscMessage.getArg(0) == 1) {
                    if (!muted) {
                        return;
                    }
                    muted = false;
                } else {
                    if (muted) {
                        return;
                    }
                    muted = true;
                }
                logger.info("Handling mute for {}  to {}", name, muted);
                return;


            }
        }
    }
}
