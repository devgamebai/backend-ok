/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.response;

public class ApiError {
    private int id;
    private String msg;

    public ApiError(int id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

