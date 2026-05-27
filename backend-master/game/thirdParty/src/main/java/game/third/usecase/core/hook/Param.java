/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.core.hook;

public class Param<T> {
    private T p;
    private int status;
    private String contentType;

    public void set(T p) {
        this.p = p;
    }

    public T get() {
        return this.p;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public String getContentType() {
        return this.contentType;
    }
}

