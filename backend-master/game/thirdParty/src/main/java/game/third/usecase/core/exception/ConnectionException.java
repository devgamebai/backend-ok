/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.core.exception;

public class ConnectionException
extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String message;
    private int code;

    public ConnectionException() {
    }

    public ConnectionException(String message, Throwable t) {
        super(message, t);
        this.message = message;
    }

    public ConnectionException(String message, int code) {
        super(message);
        this.message = message;
        this.code = code;
    }

    public ConnectionException(Throwable t) {
        super(t);
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public int getCode() {
        return this.code;
    }
}

