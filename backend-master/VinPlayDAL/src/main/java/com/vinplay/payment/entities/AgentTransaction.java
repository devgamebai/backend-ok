/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.ObjectMapper
 */
package com.vinplay.payment.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AgentTransaction {
    public String Id;
    public String CreatedAt;
    public String ModifiedAt;
    public Boolean IsDeleted;
    public String AgentId;
    public String Username;
    public String Nickname;
    public String AgentCode;
    public String RequestTime;
    public long Point;
    public long Money;
    public long Fee;
    public long Bonus;
    public int Status;
    public String FromBankNumber;
    public String ToBankNumber;
    public String Content;
    public String Description;
    public String UserApprove;

    public AgentTransaction() {
    }

    public AgentTransaction(String id, String createdAt, String modifiedAt, Boolean isDeleted, String agentId, String username, String nickname, String agentCode, String requestTime, long point, long money, long fee, long bonus, int status, String fromBankNumber, String toBankNumber, String content, String description, String userApprove) {
        this.Id = id;
        this.CreatedAt = createdAt;
        this.ModifiedAt = modifiedAt;
        this.IsDeleted = isDeleted;
        this.AgentId = agentId;
        this.Username = username;
        this.Nickname = nickname;
        this.AgentCode = agentCode;
        this.RequestTime = requestTime;
        this.Point = point;
        this.Money = money;
        this.Fee = fee;
        this.Bonus = bonus;
        this.Status = status;
        this.FromBankNumber = fromBankNumber;
        this.ToBankNumber = toBankNumber;
        this.Content = content;
        this.Description = description;
        this.UserApprove = userApprove;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            return "";
        }
    }
}

