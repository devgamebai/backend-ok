/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.dao;

import game.third.usecase.game568win.entities.UserGame568Win;

public interface UserGame568WinDao {
    public boolean checkUserExist(String var1) throws Exception;

    public UserGame568Win get(String var1) throws Exception;

    public boolean create(UserGame568Win var1) throws Exception;
}

