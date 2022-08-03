import de.kosmos_lab.kosmos.client.websocket.SimpleWebSocketEndpoint;
import de.kosmos_lab.platform.plugins.web.osc.OSCConstants;
import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import de.kosmos_lab.utils.StringFunctions;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class OSCTests {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("KioskTest");

    public static boolean filesEqual(File fileA, File fileB) throws IOException {

        InputStream inputStream1 = new FileInputStream(fileA);
        InputStream inputStream2 = new FileInputStream(fileB);

        return (IOUtils.contentEquals(inputStream1, inputStream2));
    }

    @Test
    public static void testKioskFiles() {
        try {
            URL r = OSCController.class.getResource("/web");
            if (r != null) {
                URI uri = r.toURI();
                Path myPath;
                if (uri.getScheme().equals("jar")) {
                    FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    myPath = fileSystem.getPath("/web/");
                } else {
                    myPath = Paths.get(uri);
                }
                Stream<Path> walk = Files.walk(myPath);

                for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
                    Path path = it.next();
                    String filteredName = path.toString().substring(myPath.toString().length());
                    if (filteredName.length() > 0) {

                        if (path.toFile().isFile()) {
                            logger.info("Found resource to check {} ", filteredName);
                            Assert.assertTrue(new File(String.format("web/%s", filteredName)).exists(), String.format("File did not exist! web/%s", filteredName));
                            Assert.assertTrue(filesEqual(new File(String.format("web/%s", filteredName)), path.toFile()), String.format("files did NOT match up %s", filteredName));
                        }


                    }

                }
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testWebSocketClient() {
        String id_targetList = "targetList";
        String id_auth = "auth";
        String id_show = "show";
        try {

            //prepare the websocket uri
            String wsUri = String.format("%s/osc/ws", TestBase.baseUrl.replace("http://", "ws://"));

            //create admin websocket
            WebSocketTestClient adminWebSocket = new WebSocketTestClient(new URI(wsUri));
            //create a listener that shows all messages
            adminWebSocket.addMessageHandler(Pattern.compile(".*"), new SimpleWebSocketEndpoint.MessageHandler() {
                public void handleMessage(String message) {

                    logger.info("WS Admin: {}", message);
                }
            });
            //create a listener for targetList
            adminWebSocket.addMessageHandler(Pattern.compile(String.format(".*\"type\":\"%s\".*", OSCConstants.WS_Type_targetList)), new SimpleWebSocketEndpoint.MessageHandler() {
                public void handleMessage(String message) {
                    //set the variable targetList to the current value
                    JSONObject json = new JSONObject(message);
                    adminWebSocket.set(id_targetList, json.getJSONArray("value"));
                }
            });
            adminWebSocket.addMessageHandler(Pattern.compile(".*\"type\":\"auth\\-.*"), new SimpleWebSocketEndpoint.MessageHandler() {
                public void handleMessage(String message) {

                    JSONObject json = new JSONObject(message);
                    adminWebSocket.set(id_auth, json.getString("type"));
                }
            });
            String testpw = StringFunctions.generateRandomKey();
            //will actually never happen - but better safe than sorry
            while (testpw.equals(TestBase.clientAdmin.getPassword())) {
                testpw = StringFunctions.generateRandomKey();
            }
            // auth with false password
            adminWebSocket.sendMessage(new JSONObject().put("type", OSCConstants.WS_Type_auth).put("username", TestBase.clientAdmin.getUserName()).put("password", testpw ).toString());
            Assert.assertTrue(TestBase.waitForValue(adminWebSocket.getObjects(), id_auth,OSCConstants.WS_Type_authFailed , 1000),"did not get auth failed back");
            Assert.assertFalse(TestBase.waitForValue(adminWebSocket.getObjects(), id_auth, OSCConstants.WS_Type_authSuccess, 1000),"got auth success?!");


        }  catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
    }


}
