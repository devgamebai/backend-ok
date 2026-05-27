/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.service.exception;

import java.util.Date;

public class RemoteException
extends RuntimeException {
    private static final long serialVersionUID = -7873733291888489890L;
    public final String code;
    public final String desc;
    public final Date time;

    public RemoteException(String code, String desc) {
        this(code, desc, new Date(System.currentTimeMillis()));
    }

    public RemoteException(String code, String desc, Date time) {
        super(time + " - " + code + " - " + desc);
        this.code = code;
        this.desc = desc;
        this.time = time;
    }
}

