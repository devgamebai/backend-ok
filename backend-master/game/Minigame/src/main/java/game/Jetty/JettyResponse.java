/*
 * Decompiled with CFR 0.152.
 */
package game.Jetty;

public class JettyResponse {
    public byte status;
    public String message;

    public JettyResponse(byte status, String message) {
        this.status = status;
        this.message = message;
    }

    public JettyResponse(int status, String message) {
        this((byte) status, message);
    }
}

