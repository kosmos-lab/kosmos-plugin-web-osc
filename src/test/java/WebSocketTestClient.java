import de.kosmos_lab.kosmos.client.websocket.SimpleWebSocketEndpoint;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import jakarta.websocket.ClientEndpoint;
import java.net.URI;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ClientEndpoint
public class WebSocketTestClient extends SimpleWebSocketEndpoint {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("WebSocketTestClient");

    private Queue<String> listMessages = new ConcurrentLinkedQueue();
    ConcurrentHashMap<String, Queue<MessageHandler>> mapMessageHandler = new ConcurrentHashMap<>();
    ConcurrentHashMap<Pattern, Queue<MessageHandler>> getMapMessageHandlerRegex = new ConcurrentHashMap<>();
    JSONObject json = new JSONObject();

    public void set(String key, Object value) {
        json.put(key, value);
    }

   public JSONObject getObjects()  {
        return json;
   }

    public WebSocketTestClient(URI endpointURI) {
        super(endpointURI);

        addMessageHandler(new SimpleWebSocketEndpoint.MessageHandler() {
            public void handleMessage(String message) {
                logger.info("got message: {}", message);
                listMessages.add(message);

                for (Entry<String, Queue<MessageHandler>> queueEntry : mapMessageHandler.entrySet()) {
                    try {

                        if (queueEntry.getKey().equals(message)) {
                            for (MessageHandler messageHandler : queueEntry.getValue()) {
                                messageHandler.handleMessage(message);
                            }
                            continue;
                        }


                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
                for (Entry<Pattern, Queue<MessageHandler>> queueEntry : getMapMessageHandlerRegex.entrySet()) {
                    try {
                        Matcher m = queueEntry.getKey().matcher(message);
                        if (m.matches()) {
                            for (MessageHandler messageHandler : queueEntry.getValue()) {
                                messageHandler.handleMessage(message);
                            }
                            continue;
                        }


                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        });

    }

    public void addMessageHandler(String message, MessageHandler messageHandler) {
        Queue<MessageHandler> list = mapMessageHandler.get(message);
        if (list == null) {
            list = new ConcurrentLinkedQueue();
            this.mapMessageHandler.put(message, list);
        }

        list.add(messageHandler);

    }

    public void addMessageHandler(Pattern message, MessageHandler messageHandler) {
        Queue<MessageHandler> list = getMapMessageHandlerRegex.get(message);
        if (list == null) {
            list = new ConcurrentLinkedQueue();
            this.getMapMessageHandlerRegex.put(message, list);
        }

        list.add(messageHandler);

    }

    public boolean hasMessage(String message) {
        return this.listMessages.contains(message);
    }

}
