package de.kosmos_lab.platform.plugins.web.osc.websocket;

import de.dfki.baall.helper.webserver.data.IUser;
import de.kosmos_lab.kosmos.platform.IController;
import de.kosmos_lab.kosmos.platform.web.WebServer;
import de.kosmos_lab.kosmos.platform.web.WebSocketService;
import de.kosmos_lab.platform.plugins.web.osc.OSCConstants;
import de.kosmos_lab.platform.plugins.web.osc.OSCController;
import io.netty.util.internal.ConcurrentSet;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;
import org.pf4j.Extension;
import org.pf4j.ExtensionPoint;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Extension
@ServerEndpoint("/osc/ws")
@WebSocket
public class OSCWebSocket extends WebSocketService implements ExtensionPoint {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSCWebSocket.class);
    private final Pinger pinger;
    private final WebServer server;
    private final IController controller;
    private final OSCController osc;
    ConcurrentSet<Session> sessions = new ConcurrentSet<>();
    ConcurrentHashMap<Session, String> mapSessionTarget = new ConcurrentHashMap<>();
    ConcurrentHashMap<Session, IUser> mapSessionAuth = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, Set<Session>> mapTargetSessions = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, JSONObject> mapTargetShow = new ConcurrentHashMap<>();
    public OSCWebSocket(WebServer server, IController controller) {
        this.server = server;
        this.controller = controller;
        this.osc = OSCController.getInstance(controller);
        this.osc.setWebSocket(this);
        this.pinger = new Pinger(this);
        this.pinger.start();

    }

    @Override
    @OnWebSocketConnect
    public void addWebSocketClient(Session session) {
        sessions.add(session);


    }

    @Override
    @OnWebSocketClose
    public void delWebSocketClient(Session session) {

        String t = mapSessionTarget.remove(session);
        if (t != null) {
            Set<Session> set = mapTargetSessions.get(t);
            if (set != null) {
                set.remove(session);
            }

        }
        mapSessionAuth.remove(session);
        sessions.remove(session);

    }

    public void sendToTarget(String target, JSONObject message) {
        Set<Session> s = this.mapTargetSessions.get(target);
        String type = message.optString("type", "");

        if (s != null) {
            for (Session sess : s) {

                try {

                    sess.getRemote().sendString(message.toString());

                } catch (org.eclipse.jetty.io.EofException ex) {
                    //Nothing here
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }


        }

    }



    public void broadCast(String message) {
        for (Session session : this.sessions) {
            try {
                session.getRemote().sendString(message);
            } catch (org.eclipse.jetty.io.EofException ex) {
                //Nothing here
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendMessage(JSONObject message) {
        logger.info("sending message {}", message);
        if (message.has("target")) {
            this.sendToTarget(message.getString("target"), message);


        } else {
            broadCast(message.toString());
        }

    }

    @Override
    @OnWebSocketMessage

    public void onWebSocketMessage(Session session, String message) {
        try {
            JSONObject json = new JSONObject(message);
            if (json.has("type")) {
                String type = json.getString("type");
                if (type.equals(OSCConstants.WS_Type_setTarget)) {
                    this.setTarget(session, json.getString("value"));
                } else if (type.equals(OSCConstants.WS_Type_auth)) {
                    IUser u = this.controller.tryLogin(json.getString("username"), json.getString("password"));
                    if (u != null) {
                        this.setAuth(session, u);
                        try {
                            session.getRemote().sendString(new JSONObject().put("type", OSCConstants.WS_Type_authSuccess).toString());


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            session.getRemote().sendString(new JSONObject().put("type", OSCConstants.WS_Type_authFailed).toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (JSONException ex) {
            logger.error("could not parse JSON: {}", message);
        }
    }

    private void setAuth(Session session, IUser u) {

        mapSessionAuth.put(session, u);

    }



    public Set<Entry<String, Set<Session>>> getTargetSessions() {

        return this.mapTargetSessions.entrySet();
    }

    private void setTarget(Session session, String value) {
        String oldTarget = mapSessionTarget.get(session);
        Set<Session> set;
        if (oldTarget != null) {
            set = mapTargetSessions.get(oldTarget);
            if (set != null) {
                set.remove(session);
            }
        }
        mapSessionTarget.put(session, value);
        set = mapTargetSessions.get(value);
        if (set == null) {
            set = new ConcurrentSet<Session>();
            mapTargetSessions.put(value, set);

        }
        set.add(session);
        JSONObject message = mapTargetShow.get(value);
        if (message != null) {
            try {
                session.getRemote().sendString(message.toString());
            } catch (org.eclipse.jetty.io.EofException ex) {
                //Nothing here
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }




    }


    class Pinger extends Thread {
        private final OSCWebSocket socket;

        public Pinger(OSCWebSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            while (!socket.server.isStopped()) {

                for (Session s : socket.sessions) {
                    try {
                        s.getRemote().sendString(new JSONObject().put("type", "ping").toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(30000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
