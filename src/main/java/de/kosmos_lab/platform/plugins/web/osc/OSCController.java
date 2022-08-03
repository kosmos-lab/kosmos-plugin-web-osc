package de.kosmos_lab.platform.plugins.web.osc;


import de.kosmos_lab.kosmos.data.Device;
import de.kosmos_lab.kosmos.platform.IController;
import de.kosmos_lab.kosmos.platform.smarthome.CommandInterface;
import de.kosmos_lab.kosmos.platform.smarthome.CommandSourceName;
import de.kosmos_lab.platform.plugins.web.osc.data.OSCOutput;
import de.kosmos_lab.platform.plugins.web.osc.data.OSCSlider;
import de.kosmos_lab.platform.plugins.web.osc.websocket.OSCWebSocket;
import de.kosmos_lab.utils.FileUtils;
import de.kosmos_lab.utils.KosmosFileUtils;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashSet;

public class OSCController implements OSCListener, CommandInterface {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCController.class);

    private static OSCController instance;

    private final IController controller;
    private final File configFile;
    private OSCClient osc;

    private JSONObject config;
    private OSCWebSocket websocket;
    HashSet<OSCSlider> sliders = new HashSet<>();
    HashSet<OSCOutput> outputs = new HashSet<>();
    HashSet<OSCListener> handler = new HashSet<>();

    public OSCController(IController controller) {
        this.controller = controller;
        File dir = controller.getFile("config/osc");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logger.error("could not create osc directory");
                controller.stop();

            }
        }
        this.configFile = controller.getFile("config/osc/config.json");
        if (configFile.exists()) {
            try {
                this.config = new JSONObject(FileUtils.readFile(configFile));
            } catch (JSONException ex) {
                ex.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (config == null) {
            config = new JSONObject();
        }
        if ( config.has("port") && config.has("host")) {
            this.osc = new OSCClient(this, config.getString("host"),config.getInt("port"));
            osc.start();
        }

        this.save();

    }

    public static synchronized OSCController getInstance(IController controller) {

        if (instance == null) {
            instance = new OSCController(controller);
            controller.addCommandInterface(instance);
        }

        return instance;
    }

    private void save() {
        KosmosFileUtils.writeToFile(configFile, config.toString());
    }

    public void sendMessage(JSONObject message) {
        if (websocket != null) {
            this.websocket.sendMessage(message);
        }

    }

    public void setWebSocket(OSCWebSocket webSocket) {
        this.websocket = webSocket;


    }


    public JSONObject getSliders() {
        return null;
    }

    @Override
    public void messageReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
        for ( OSCListener listener : handler) {
            listener.messageReceived(oscMessage,socketAddress,l);
        }
        System.out.print(oscMessage.getName());
        for (int i = 0; i < oscMessage.getArgCount(); i++) {
            System.out.print(" ");
            System.out.print(oscMessage.getArg(i));

        }
        System.out.println();
    }

    @Override
    public void deviceAdded(@Nullable CommandInterface commandInterface, @NotNull Device device, @NotNull CommandSourceName commandSourceName) {

    }

    @Override
    public void deviceRemoved(@Nullable CommandInterface commandInterface, @NotNull Device device, @NotNull CommandSourceName commandSourceName) {

    }

    @Override
    public void deviceUpdate(@Nullable CommandInterface commandInterface, @NotNull Device device, @Nullable String s, @NotNull CommandSourceName commandSourceName) {

    }

    @Override
    public void stop() {
        if ( this.osc != null ) {
            this.osc.stopClient();
        }
    }

    @Override
    public String getSourceName() {
        return "osc";
    }

    public void send(OSCMessage oscMessage) {
        if ( osc != null ) {
            osc.send(oscMessage);
        }
    }

    public void addSlider(OSCSlider oscSlider) {
        this.sliders.add(oscSlider);
    }

    public void addOutput(OSCOutput oscOutput) {
        this.outputs.add(oscOutput);
        this.handler.add(oscOutput);
    }
}
