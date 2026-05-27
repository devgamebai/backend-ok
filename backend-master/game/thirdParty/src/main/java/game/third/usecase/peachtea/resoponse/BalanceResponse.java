/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.peachtea.resoponse;

public class BalanceResponse {
    private int agentId;
    private int userId;
    private String nickname;
    private int accountBalance;

    public int getAgentId() {
        return this.agentId;
    }

    public void setAgentId(int agentId) {
        this.agentId = agentId;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return this.nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAccountBalance() {
        return this.accountBalance;
    }

    public void setAccountBalance(int accountBalance) {
        this.accountBalance = accountBalance;
    }
}

