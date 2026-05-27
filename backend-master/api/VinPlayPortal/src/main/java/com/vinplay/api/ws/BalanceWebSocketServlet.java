package com.vinplay.api.ws;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time balance push to portal clients.
 * Path: /ws/balance?token={accessToken}
 *
 * Auth: accessToken query param -> Hazelcast cacheToken lookup -> nickname.
 * Tracks nickname->session in ConcurrentHashMap for push from RMQ consumer.
 */
public class BalanceWebSocketServlet extends WebSocketServlet {

    private static final Logger logger = Logger.getLogger("portal");

    /** nickname -> active WebSocket session. One session per user (last-write-wins). */
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * accessToken -> BalanceSocket. Used by {@link #kickByToken(String, String)} so
     * that when Hazelcast {@code cacheToken} revokes a specific token (SUN-832
     * hardening), we can close ONLY the socket tied to that token — even if a
     * newer device already holds a live socket for the same nickname.
     */
    private static final ConcurrentHashMap<String, BalanceSocket> tokenToSocket = new ConcurrentHashMap<>();

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(300000); // 5 min idle
        factory.getPolicy().setMaxTextMessageSize(65536);
        factory.setCreator(new WebSocketCreator() {
            @Override
            public Object createWebSocket(ServletUpgradeRequest req, org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse resp) {
                return new BalanceSocket();
            }
        });
    }

    /**
     * Get the session map for external push (used by PortalBalanceConsumer).
     */
    public static ConcurrentHashMap<String, Session> getSessions() {
        return sessions;
    }

    /**
     * Send a JSON message to a specific nickname's WebSocket session.
     * Returns true if sent, false if user not connected or send failed.
     */
    public static boolean sendToUser(String nickname, String json) {
        Session session = sessions.get(nickname);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            session.getRemote().sendString(json);
            return true;
        } catch (IOException e) {
            logger.warn("WS send failed for " + nickname + ": " + e.getMessage());
            sessions.remove(nickname, session);
            return false;
        }
    }

    /**
     * Kick the socket that authenticated with {@code accessToken} — SUN-832
     * hardening. Called from a Hazelcast {@code cacheToken} entryRemoved
     * listener in {@link com.vinplay.api.server.VinPlayPortal} whenever a
     * token is revoked (e.g., second-device login, admin kick, password
     * change).
     *
     * <p>Emits a JSON force-logout message and closes the socket with code
     * 4002. The client handler should show a popup and return to login.
     *
     * <p>Only the session tied to this specific token is closed — a newer
     * device with a different token remains untouched.
     */
    public static boolean kickByToken(String accessToken, String reason) {
        if (accessToken == null || accessToken.isEmpty()) return false;
        BalanceSocket socket = tokenToSocket.get(accessToken);
        if (socket == null) return false;
        Session session = socket.getSession();
        if (session == null || !session.isOpen()) {
            tokenToSocket.remove(accessToken, socket);
            return false;
        }
        try {
            String json = "{\"type\":\"force_logout\",\"reason\":\""
                    + (reason != null ? reason : "DUPLICATE_LOGIN") + "\"}";
            session.getRemote().sendString(json);
        } catch (IOException e) {
            logger.warn("WS force_logout send failed for token=" + accessToken + ": " + e.getMessage());
        }
        try { session.close(4002, "force_logout"); } catch (Exception ignore) {}
        tokenToSocket.remove(accessToken, socket);
        if (socket.nickname != null) {
            sessions.remove(socket.nickname, session);
        }
        logger.info("WS balance force_logout token=" + accessToken
                + " nick=" + socket.nickname + " reason=" + reason);
        return true;
    }

    /**
     * Inner WebSocket handler for each connection.
     */
    public static class BalanceSocket extends WebSocketAdapter {

        String nickname;
        String accessToken; // SUN-832: track so kickByToken can close this exact socket

        @Override
        public void onWebSocketConnect(Session session) {
            super.onWebSocketConnect(session);
            try {
                // Extract token from query param
                Map<String, List<String>> params = session.getUpgradeRequest().getParameterMap();
                List<String> tokenList = params.get("token");
                String token = (tokenList != null && !tokenList.isEmpty()) ? tokenList.get(0) : null;

                if (token == null || token.isEmpty()) {
                    sendAndClose(session, "{\"type\":\"auth_failed\",\"message\":\"Missing token\"}");
                    return;
                }

                // Look up nickname from cacheToken
                HazelcastInstance hz = HazelcastClientFactory.getInstance();
                IMap<String, String> tokenMap = hz.getMap("cacheToken");
                String nick = tokenMap.get(token);

                if (nick == null || nick.isEmpty()) {
                    sendAndClose(session, "{\"type\":\"auth_failed\",\"message\":\"Invalid token\"}");
                    return;
                }

                this.nickname = nick;
                this.accessToken = token;

                // SUN-832: index this socket by its token so the Hazelcast
                // entryRemoved listener can kick the exact session that holds
                // a revoked token (a newer device with a different token will
                // not be collateral damage).
                BalanceSocket prevTokenSocket = tokenToSocket.put(token, this);
                if (prevTokenSocket != null && prevTokenSocket != this) {
                    Session oldTokenSession = prevTokenSocket.getSession();
                    if (oldTokenSession != null && oldTokenSession.isOpen()) {
                        try { oldTokenSession.close(4001, "replaced"); } catch (Exception ignore) {}
                    }
                }

                // Close any previous session for this user
                Session prev = sessions.put(nickname, session);
                if (prev != null && prev.isOpen() && prev != session) {
                    try { prev.close(4001, "replaced"); } catch (Exception ignore) {}
                }

                // Get current balance from Hazelcast users map
                IMap<String, UserCacheModel> userMap = hz.getMap("users");
                UserCacheModel cache = userMap.get(nickname);

                long vin = 0;
                long xu = 0;
                if (cache != null) {
                    vin = cache.getVin();
                    xu = cache.getXu();
                }

                // Send auth_ok with current balance
                String authMsg = "{\"type\":\"auth_ok\",\"nickname\":\"" + nickname
                        + "\",\"vin\":" + vin + ",\"xu\":" + xu + "}";
                session.getRemote().sendString(authMsg);

                logger.info("WS balance connected: " + nickname + " vin=" + vin + " xu=" + xu);

            } catch (Exception e) {
                logger.error("WS balance onConnect error", e);
                try {
                    sendAndClose(session, "{\"type\":\"auth_failed\",\"message\":\"Internal error\"}");
                } catch (Exception ignore) {}
            }
        }

        @Override
        public void onWebSocketText(String message) {
            // Client can send "ping" to keep alive; respond with "pong"
            if ("ping".equalsIgnoreCase(message)) {
                try {
                    Session s = getSession();
                    if (s != null && s.isOpen()) {
                        s.getRemote().sendString("{\"type\":\"pong\"}");
                    }
                } catch (IOException e) {
                    logger.warn("WS pong send error: " + e.getMessage());
                }
            }
            // Ignore other messages — this is a push-only channel
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            super.onWebSocketClose(statusCode, reason);
            if (nickname != null) {
                // Only remove if it's still our session (avoid removing a replacement)
                sessions.remove(nickname, getSession());
                logger.info("WS balance disconnected: " + nickname + " code=" + statusCode);
            }
            if (accessToken != null) {
                tokenToSocket.remove(accessToken, this);
            }
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            super.onWebSocketError(cause);
            if (nickname != null) {
                logger.warn("WS balance error for " + nickname + ": " + cause.getMessage());
                sessions.remove(nickname, getSession());
            }
            if (accessToken != null) {
                tokenToSocket.remove(accessToken, this);
            }
        }

        private void sendAndClose(Session session, String message) {
            try {
                session.getRemote().sendString(message);
                session.close(4000, "auth_failed");
            } catch (IOException e) {
                logger.warn("WS sendAndClose error: " + e.getMessage());
            }
        }
    }
}
