import de.kosmos_lab.kosmos.client.KosmoSClient;
import de.kosmos_lab.kosmos.platform.KosmoSController;
import de.kosmos_lab.kosmos.platform.persistence.Constants.RunMode;
import de.kosmos_lab.utils.FileUtils;
import de.kosmos_lab.utils.JSONChecker;
import de.kosmos_lab.utils.KosmosFileUtils;
import de.kosmos_lab.utils.StringFunctions;
import de.kosmos_lab.utils.exceptions.CompareException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@SuppressFBWarnings("MS_CANNOT_BE_FINAL")
public class TestBase {
    final public static ConcurrentHashMap<String, JSONObject> jsonCache = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("Test");
    public static String pass;

    static KosmoSController controller;
    static KosmoSClient clientAdmin;
    static KosmoSClient clientUser;


    static KosmoSClient clientFakeUser;
    static String baseUrl = "";
    @SuppressFBWarnings("MS_SHOULD_BE_FINAL")
    static KosmoSClient clientNoLogin;


    @SuppressFBWarnings({"UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"})
    @BeforeSuite
    public static void prepare() {

        String[] params = new String[3];

        params[0] = "mvn";
        params[1] = "package";
        params[2] = "-DskipTests=True";

        try {
            final Process p = Runtime.getRuntime().exec(params);
            Thread thread = new Thread() {
                public void run() {
                    String line = null;
                    BufferedReader input =
                            new BufferedReader
                                    (new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));

                    while (true) {
                        try {
                            if ((line = input.readLine()) == null) break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println(line);
                    }


                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();
            int result = p.waitFor();
            thread.join();
            if (result != 0) {
                System.out.println("Process failed with status: " + result);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        File pluginDir = new File("plugins");
        if (pluginDir.exists()) {
            File[] fileList = pluginDir.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    if (f.isFile() && f.getName().endsWith(".zip")) {
                        logger.info("found {} deleting it from plugins folder", f);
                        if(!f.delete()) {
                            logger.error("could not delete file {}",f);
                        }
                    }


                }
            }
        } else {
            if(!pluginDir.mkdirs()) {
                logger.error("could not create plugin directory!");
            }
        }
        File[] fileList = new File("target/").listFiles();
        if (fileList != null) {
            for (File f : fileList) {
                if (f.isFile() && f.getName().endsWith(".zip")) {
                    logger.info("found {} copying it to plugins folder", f);
                    try {
                        org.apache.commons.io.FileUtils.copyFile(f, new File(
                                String.format("%s/%s", "plugins", f.getName())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        File testdb = KosmoSController.getFile("db/db.sqlite", RunMode.TEST);
        if (testdb.exists()) {
            //Assert.assertTrue(testdb.delete(),"could not delete old test db!!");
            logger.info("deleting old DB {}", testdb);
            if (!testdb.delete()) {
                // we don't actually care about the return value, the next loop will get it deleted
                // this might happen if we start the test again while the previous one is still shutting down
            }

        }
        if (testdb.exists()) {
            for (int i = 0; i < 12; i++) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {

                    logger.error("could not sleep!", e);
                }
                if (testdb.delete()) {
                    break;
                }

            }
        }
        if (testdb.exists()) {
            Assert.fail("could not delete old test db!!");
        }



        setup();

    }


    public static void setup() {
        File testConf = KosmoSController.getFile("config.json", RunMode.TEST);
        JSONObject config = new JSONObject();
        int port = 18086;
        config.put("webserver", new JSONObject().put("port", port));

        baseUrl = "http://localhost:" + port;


        config.put("mqtt", new JSONObject().put("port", 1884));
        config.put("sql", new JSONObject().put("url", String.format("jdbc:sqlite:%s", KosmoSController.getFile("db/db.sqlite", RunMode.TEST))));
        KosmosFileUtils.writeToFile(testConf, config.toString(2));
        try {

            clientAdmin = new KosmoSClient(baseUrl, "admin", StringFunctions.generateRandomKey());
            clientUser = new KosmoSClient(baseUrl, "user", StringFunctions.generateRandomKey());


            clientFakeUser = new KosmoSClient(baseUrl, "fakeuser2", "test");
            clientNoLogin = new KosmoSClient(baseUrl, null, null);
            FileUtils.writeToFile(KosmoSController.getFile("users.json", RunMode.TEST),
                    new JSONObject().
                            put(clientAdmin.getUserName(), clientAdmin.getPassword()).
                            put(clientUser.getUserName(), clientUser.getPassword()).
                            toString());

        } catch (Exception e) {
            logger.error("could not create users and write the information to a seperate file!", e);
        }
        controller = new KosmoSController(testConf, RunMode.TEST);
        controller.addUser(clientAdmin.getUserName(), clientAdmin.getPassword(), 1000);
        controller.addUser(clientUser.getUserName(), clientUser.getPassword(), 1);
        Assert.assertNotNull(controller.tryLogin(clientAdmin.getUserName(),clientAdmin.getPassword()));
        Assert.assertNotNull(controller.tryLogin(clientUser.getUserName(),clientUser.getPassword()));
    }

    /**
     * this method is used to give the system some time to react to an order via a slow..ish medium. It checks the value
     * every 500ms and returns true as soon as it changed, or false if it never changed and the timeout was reached
     *
     * @param jsonObject the JSONObject to "monitor"
     * @param key        the key to watch
     * @param expected   the expected value
     * @param waittime   waittime in ms
     *
     * @return
     */
    public static boolean waitForValue(JSONObject jsonObject, String key, Object expected, long waittime) {
        long started = System.currentTimeMillis();
        CompareException laste = null;
        while (true) {
            try {
                if (JSONChecker.checkValue(jsonObject, key, expected)) {
                    return true;
                }
            } catch (CompareException e) {
                //dont spam the log for now
                laste = e;
            } catch (Exception e) {
                //e.printStackTrace();
                logger.warn("Exception while comparing {} to {}: {}", key, expected, e.getMessage());
            }
            if (jsonObject.has(key)) {
                logger.info("{} vs {}", jsonObject.get(key), expected);
            } else {
                logger.info("no {} in {}", key, jsonObject);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("could not sleep!", e);
            }
            long delta = System.currentTimeMillis() - started;
            if (delta > waittime) {
                if (laste != null) {
                    logger.warn("could not match {} to {}: {}", key, expected, laste.getMessage());
                }
                return false;
            }
        }
    }

    public static void startIfNeeded() {
        if (controller == null) {
            prepare();
        }

    }


    @AfterSuite
    public void cleanup(ITestContext context) {
        if (controller != null) {

            controller.stop();
        }

    }


}
