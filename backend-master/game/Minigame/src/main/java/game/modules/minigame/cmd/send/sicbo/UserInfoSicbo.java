/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame.cmd.send.sicbo;

public class UserInfoSicbo {
    private String userName;
    private String balance;

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBalance() {
        return this.balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public UserInfoSicbo(String userName, String balance) {
        this.userName = userName;
        this.balance = balance;
    }
}

