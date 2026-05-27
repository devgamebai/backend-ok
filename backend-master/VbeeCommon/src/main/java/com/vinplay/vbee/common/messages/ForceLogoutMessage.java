package com.vinplay.vbee.common.messages;

/**
 * RMQ message published by the portal after a successful login that
 * invalidates an existing session for the same account (SUN-767).
 *
 * <p>Consumed by the game servers (see {@code game.queue.ForceLogoutConsumer}
 * in Minigame, and the parallel consumer in agency pipelines). On receive,
 * the game server looks up any bitzero sessions still alive for {@code nickname},
 * sends them a {@code ForceLogoutMsg} bitzero packet, and disconnects them so
 * the old device returns to the login screen.
 *
 * <p>Reason codes (string, not enum, to keep wire forward-compatible):
 * <ul>
 *   <li>{@code DUPLICATE_LOGIN} — another device just logged into the same
 *       account (default case for SUN-767).
 *   <li>{@code ADMIN_KICK}      — reserved for future admin force-logout flow.
 *   <li>{@code SECURITY}        — reserved for password change / suspicious activity.
 * </ul>
 */
public class ForceLogoutMessage extends BaseMessage {
    private static final long serialVersionUID = 1L;

    public String nickname;
    public String reason;       // one of DUPLICATE_LOGIN / ADMIN_KICK / SECURITY
    public String newLoginIp;   // IP of the new session that kicked the old (nullable)
    public long   newLoginTime; // epoch millis of the new login

    public ForceLogoutMessage() {}

    public ForceLogoutMessage(String nickname, String reason, String newLoginIp, long newLoginTime) {
        this.nickname = nickname;
        this.reason = reason;
        this.newLoginIp = newLoginIp;
        this.newLoginTime = newLoginTime;
    }
}
