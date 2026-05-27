package com.vinplay.dal.entities.agent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AgentCommissionDaily {
    private String date;          // yyyy-MM-dd
    private String userNickname;
    private double userCommissionRate;
    private String referralCode;  // agent code the user belongs to
    private long totalBet;        // total bet in day
    private long totalBetCasino;
    private long totalBetSport;
    private long totalBetGame;
    private long userCommission;  // amount user earns as commission
    private List<AgentDistribution> distributions; // commission for each agent level
    private Map<String, GameDetail> gameDetails;   // per-game commission breakdown
    private Date createTime;

    public static class GameDetail {
        private int gameId;
        private String gameName;
        private long betAmount;
        private double userRate;
        private long userCommission;
        private List<AgentDistribution> agentDistributions;

        public GameDetail() {
        }

        public GameDetail(int gameId, String gameName, long betAmount, double userRate,
                          long userCommission, List<AgentDistribution> agentDistributions) {
            this.gameId = gameId;
            this.gameName = gameName;
            this.betAmount = betAmount;
            this.userRate = userRate;
            this.userCommission = userCommission;
            this.agentDistributions = agentDistributions != null ? agentDistributions : new ArrayList<>();
        }

        public int getGameId() { return gameId; }
        public void setGameId(int gameId) { this.gameId = gameId; }
        public String getGameName() { return gameName; }
        public void setGameName(String gameName) { this.gameName = gameName; }
        public long getBetAmount() { return betAmount; }
        public void setBetAmount(long betAmount) { this.betAmount = betAmount; }
        public double getUserRate() { return userRate; }
        public void setUserRate(double userRate) { this.userRate = userRate; }
        public long getUserCommission() { return userCommission; }
        public void setUserCommission(long userCommission) { this.userCommission = userCommission; }
        public List<AgentDistribution> getAgentDistributions() { return agentDistributions; }
        public void setAgentDistributions(List<AgentDistribution> agentDistributions) { this.agentDistributions = agentDistributions; }
    }

    public static class AgentDistribution {
        private String agentCode;
        private String agentNickname;
        private int agentLevel;
        private double agentRate;     // agent's commission_rate
        private double earnRate;      // effective rate this agent earns
        private long commission;      // amount this agent earns

        public AgentDistribution() {
        }

        public AgentDistribution(String agentCode, String agentNickname, int agentLevel, double agentRate, double earnRate, long commission) {
            this.agentCode = agentCode;
            this.agentNickname = agentNickname;
            this.agentLevel = agentLevel;
            this.agentRate = agentRate;
            this.earnRate = earnRate;
            this.commission = commission;
        }

        public String getAgentCode() {
            return agentCode;
        }

        public void setAgentCode(String agentCode) {
            this.agentCode = agentCode;
        }

        public String getAgentNickname() {
            return agentNickname;
        }

        public void setAgentNickname(String agentNickname) {
            this.agentNickname = agentNickname;
        }

        public int getAgentLevel() {
            return agentLevel;
        }

        public void setAgentLevel(int agentLevel) {
            this.agentLevel = agentLevel;
        }

        public double getAgentRate() {
            return agentRate;
        }

        public void setAgentRate(double agentRate) {
            this.agentRate = agentRate;
        }

        public double getEarnRate() {
            return earnRate;
        }

        public void setEarnRate(double earnRate) {
            this.earnRate = earnRate;
        }

        public long getCommission() {
            return commission;
        }

        public void setCommission(long commission) {
            this.commission = commission;
        }
    }

    public AgentCommissionDaily() {
    }

    public AgentCommissionDaily(String date, String userNickname, double userCommissionRate, String referralCode,
                                long totalBet, long totalBetCasino, long totalBetSport, long totalBetGame,
                                long userCommission, List<AgentDistribution> distributions, Date createTime) {
        this.date = date;
        this.userNickname = userNickname;
        this.userCommissionRate = userCommissionRate;
        this.referralCode = referralCode;
        this.totalBet = totalBet;
        this.totalBetCasino = totalBetCasino;
        this.totalBetSport = totalBetSport;
        this.totalBetGame = totalBetGame;
        this.userCommission = userCommission;
        this.distributions = distributions != null ? distributions : new ArrayList<>();
        this.createTime = createTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public double getUserCommissionRate() {
        return userCommissionRate;
    }

    public void setUserCommissionRate(double userCommissionRate) {
        this.userCommissionRate = userCommissionRate;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public long getTotalBet() {
        return totalBet;
    }

    public void setTotalBet(long totalBet) {
        this.totalBet = totalBet;
    }

    public long getTotalBetCasino() {
        return totalBetCasino;
    }

    public void setTotalBetCasino(long totalBetCasino) {
        this.totalBetCasino = totalBetCasino;
    }

    public long getTotalBetSport() {
        return totalBetSport;
    }

    public void setTotalBetSport(long totalBetSport) {
        this.totalBetSport = totalBetSport;
    }

    public long getTotalBetGame() {
        return totalBetGame;
    }

    public void setTotalBetGame(long totalBetGame) {
        this.totalBetGame = totalBetGame;
    }

    public long getUserCommission() {
        return userCommission;
    }

    public void setUserCommission(long userCommission) {
        this.userCommission = userCommission;
    }

    public List<AgentDistribution> getDistributions() {
        return distributions;
    }

    public void setDistributions(List<AgentDistribution> distributions) {
        this.distributions = distributions;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Map<String, GameDetail> getGameDetails() {
        return gameDetails;
    }

    public void setGameDetails(Map<String, GameDetail> gameDetails) {
        this.gameDetails = gameDetails;
    }
}
