package de.kosmos_lab.platform.plugins.web.osc.data;

import de.kosmos_lab.platform.plugins.web.osc.OSCConstants;
import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import de.sciss.net.OSCMessage;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class OSCOutput extends OSCChannel {
    public static final int MAX_CHANNELS = 33;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCOutput.class);

    final Pattern patternChannel = Pattern.compile("^/ch/(?<channel>\\d*)/mix/(?<aux>\\d*)/level$");
    //final Pattern patternChannel = Pattern.compile("^/ch/(?<channel>\\d*)/mix/fader$");


    private final String[] levelFormat;
    float[] levels = new float[MAX_CHANNELS];
    private boolean muted = false;
    private int level = 0;

    public OSCOutput(OSCController controller, String name, String[] levelFormat, String[] muteFormat) {
        super(controller, name);
        this.levelFormat = levelFormat;
        this.muteFormat = muteFormat;

        Pattern[] levelPattern = new Pattern[levelFormat.length];
        Pattern[] mutePattern = new Pattern[muteFormat.length];
        for (int i = 0; i < levelFormat.length; i++) {
            levelPattern[i] = Pattern.compile("^" + levelFormat[i].replaceAll("%02d", "(\\\\d*)") + "$");
        }
        for (int i = 0; i < muteFormat.length; i++) {
            mutePattern[i] = Pattern.compile("^" + muteFormat[i].replaceAll("%02d", "(\\\\d*)") + "$");
        }
        this.addListener(new OSCChannelMuteListener(this, mutePattern));
        this.addListener(new OSCChannelLevelListener(this, levelPattern));

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
            logger.info("skipping set for {} to {} value is already {}",getName(),value,levels[channel]);
            return false;
        }
        for (String f : levelFormat) {
            controller.send(new OSCMessage(String.format(f, channel), new Object[]{value}));
            controller.sendToWebsocketConnections(new JSONObject().
                                                put("type", OSCConstants.WS_Type_value).
                                                put("output",name).
                                                put("value",value).
                                                put("input",controller.getInputName(channel)).toString());
        }

        return true;

    }

    public float getLevel(OSCInput input) {
        for (int channel : input.getChannel()) {
            return this.levels[channel];
        }
        return 0;
    }

    public void setLevel(OSCInput input, float value) {
        for (int channel : input.getChannel()) {
            setLevel(channel, value);
        }

    }



    /*@Override
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
    public String getName() {
        return this.name;
    }


    */


}
