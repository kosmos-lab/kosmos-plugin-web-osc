package de.kosmos_lab.platform.plugins.web.osc.data;

import de.kosmos_lab.platform.plugins.web.osc.OSCConstants;
import de.sciss.net.OSCMessage;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSCChannelLevelListener implements OSCChannelListener {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCChannelLevelListener.class);

    private final OSCOutput channel;
    private final Pattern[] levelPattern;

    public OSCChannelLevelListener(OSCOutput channel, Pattern[] levelPattern) {
        this.channel = channel;
        this.levelPattern = levelPattern;
    }

    @Override
    public void messageReceived(OSCMessage oscMessage) {
        for (Pattern p : levelPattern) {
            //logger.info("trying to match {} to {}",p.pattern(),oscMessage.getName());
            Matcher m = p.matcher(oscMessage.getName());
            if (m.matches()) {
                logger.info("Handling set level for {} Channel {} to {}", channel.name, m.group(1), (Float) oscMessage.getArg(0));
                logger.info("Handling set level for {} Channel {} to {}", channel.name, m.group(1), (Float) oscMessage.getArg(0));
                int channelId = Integer.parseInt(m.group(1));
                float value = (Float) oscMessage.getArg(0);
                channel.levels[channelId-1] = value;
                channel.controller.sendToWebsocketConnections(new JSONObject().
                                                    put("type", OSCConstants.WS_Type_value).
                                                    put("output",channel.name).
                                                    put("value",value).
                                                    put("input",channel.controller.getInputName(channelId)).toString());
                return;


            }
        }
    }
}