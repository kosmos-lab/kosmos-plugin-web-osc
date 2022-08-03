package de.kosmos_lab.platform.plugins.web.osc.data;

import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import de.sciss.net.OSCMessage;


/*
 * * OSCController has 5 Stereo Outputs
 *
 *
 */
public class OSCSlider {

    private final OSCController controller;
    private int level = 0;
    private String path;

    public OSCSlider(OSCController controller, String path) {
        this.controller = controller;
        this.path = path;
        this.controller.addSlider(this);
    }

    void doSet() {
        this.controller.send(new OSCMessage(path, new Object[]{level}));
    }

    public boolean set(int value) {
        if (value == level) {
            return false;
        }
        doSet();
        return true;
    }

    public boolean isMe(String path) {
        if (this.path.equalsIgnoreCase(path)) {
            return true;
        }
        return false;
    }

}
