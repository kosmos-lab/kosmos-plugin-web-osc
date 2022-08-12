package de.kosmos_lab.platform.plugins.web.osc;


import de.kosmos_lab.kosmos.data.Device;
import de.kosmos_lab.kosmos.platform.IController;
import de.kosmos_lab.kosmos.platform.smarthome.CommandInterface;
import de.kosmos_lab.kosmos.platform.smarthome.CommandSourceName;
import de.kosmos_lab.platform.plugins.web.osc.data.OSCInput;
import de.kosmos_lab.platform.plugins.web.osc.data.OSCOutput;
import de.kosmos_lab.platform.plugins.web.osc.websocket.OSCWebSocket;
import de.kosmos_lab.utils.FileUtils;
import de.kosmos_lab.utils.KosmosFileUtils;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSCController implements OSCListener, CommandInterface {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCController.class);

    private static OSCController instance;

    private final IController controller;
    private final File configFile;
    ConcurrentHashMap<String, OSCOutput> outputs = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, OSCInput> inputs = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, OSCInput> inputsByChannel = new ConcurrentHashMap<>();

    //we absolutely need those keys to be thread safe, and this one way to get a thread safe "hashset" without very much overhead
    ConcurrentHashMap<OSCListener, Boolean> handler = new ConcurrentHashMap<>();
    private OSCClient osc;
    private JSONObject config;
    private OSCWebSocket websocket;
    private String model;
    private String firmware;

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

        if (config.has("port") && config.has("host")) {
            this.osc = new OSCClient(this, config.getString("host"), config.getInt("port"));
            osc.start();
        }
        if (config.has("outputs")) {
            JSONArray outputsArray = config.optJSONArray("outputs");
            if (outputsArray != null) {
                Pattern auxPattern = Pattern.compile("^aux(\\d*)$");
                for (int i = 0; i < outputsArray.length(); i++) {
                    JSONObject port = outputsArray.getJSONObject(i);
                    String type = port.getString("type");
                    String[] levelFader = null;
                    String[] muteFader = null;
                    if (type != null) {
                        if (type.equalsIgnoreCase("main")) {
                            muteFader = new String[]{"/lr/mix/on"};
                            levelFader = new String[]{"/ch/%02d/mix/fader"};
                        } else {
                            Matcher m = auxPattern.matcher(type);
                            if (m.matches()) {
                                int id = Integer.parseInt(m.group(1));
                                muteFader = new String[]{String.format("/bus/%d/mix/on", id)};
                                levelFader = new String[]{"/ch/%02d/" + String.format("mix/%02d/level", id)};
                            }
                        }
                    }
                    if (levelFader != null && muteFader != null) {
                        new OSCOutput(this, port.getString("name"), levelFader, muteFader);
                    }
                }
            }
        }
        if (config.has("inputs")) {
            JSONArray inputsArray = config.optJSONArray("inputs");
            if (inputsArray != null) {

                try {
                    for (int i = 0; i < inputsArray.length(); i++) {
                        JSONObject port = inputsArray.getJSONObject(i);

                        int[] channel = null;
                        Object c = port.opt("channel");
                        if (c instanceof Integer) {
                            channel = new int[]{(int) c};
                        }
                        if (c instanceof JSONArray) {
                            JSONArray a = (JSONArray) c;
                            channel = new int[a.length()];
                            for (int j = 0; j < a.length(); j++) {
                                channel[j] = a.getInt(j);
                            }
                        }

                        if (channel != null) {
                            new OSCInput(this, port.getString("name"), channel);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

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
        KosmosFileUtils.writeToFile(configFile, config.toString(4));
    }


    public void setWebSocket(OSCWebSocket webSocket) {
        this.websocket = webSocket;


    }


    @Override
    public void messageReceived(OSCMessage oscMessage, SocketAddress socketAddress, long l) {
        for (OSCListener listener : handler.keySet()) {
            listener.messageReceived(oscMessage, socketAddress, l);
        }
        if (oscMessage.getName().equals("/info")) {
            this.model = String.valueOf(oscMessage.getArg(2));
            this.firmware = String.valueOf(oscMessage.getArg(3));

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
//we can safely ignore this here, we just want to react to the stop here
    }

    @Override
    public void deviceRemoved(@Nullable CommandInterface commandInterface, @NotNull Device device, @NotNull CommandSourceName commandSourceName) {
//we can safely ignore this here, we just want to react to the stop here
    }

    @Override
    public void deviceUpdate(@Nullable CommandInterface commandInterface, @NotNull Device device, @Nullable String s, @NotNull CommandSourceName commandSourceName) {
//we can safely ignore this here, we just want to react to the stop here
    }

    @Override
    public void stop() {
        if (this.osc != null) {
            this.osc.stopClient();
        }
    }

    @Override
    public String getSourceName() {
        return "osc";
    }

    public void send(OSCMessage oscMessage) {
        if (osc != null) {
            osc.send(oscMessage);
        }
    }


    public void addOutput(OSCOutput oscOutput) {
        this.outputs.put(oscOutput.getName().toLowerCase(), oscOutput);
        this.handler.put(oscOutput, true);
    }

    public void addInput(OSCInput oscInput) {
        this.inputs.put(oscInput.getName().toLowerCase(), oscInput);
        for (int channel : oscInput.getChannel()) {
            this.inputsByChannel.put(channel, oscInput);
        }
        this.handler.put(oscInput, true);

    }

    public OSCOutput getOutput(String name) {
        return this.outputs.get(name.toLowerCase());
    }

    public OSCInput getInput(String name) {
        return this.inputs.get(name.toLowerCase());
    }

    public JSONObject getValues() {
        JSONObject json = new JSONObject();
        JSONArray inputJson = new JSONArray();
        for (OSCInput input : inputs.values()) {
            inputJson.put(new JSONObject().put("name", input.getName()).put("muted", input.isMuted()));
        }
        JSONArray outputJSON = new JSONArray();
        for (OSCOutput output : outputs.values()) {
            JSONObject j = new JSONObject().put("name", output.getName()).put("muted", output.isMuted());
            JSONObject levels = new JSONObject();
            for (OSCInput input : inputs.values()) {
                levels.put(input.getName(), output.getLevel(input));

            }
            j.put("levels", levels);
            outputJSON.put(j);
        }
        json.put("inputs", inputJson);
        json.put("outputs", outputJSON);

        return json;

    }

    public void sendToWebsocketConnections(String message) {
        if (this.websocket != null) {
            this.websocket.broadCast(message);
        }
    }

    public OSCInput getInputByChannel(int channel) {

        return this.inputsByChannel.get(channel);
    }

    public String getInputName(int channel) {
        OSCInput input = getInputByChannel(channel);
        if (input != null) {
            return input.getName();
        }
        return null;
    }
}
