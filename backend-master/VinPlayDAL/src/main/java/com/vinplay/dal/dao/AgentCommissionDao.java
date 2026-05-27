package com.vinplay.dal.dao;

import com.vinplay.dal.entities.agent.AgentCommissionDaily;
import java.util.List;

public interface AgentCommissionDao {
    void upsert(AgentCommissionDaily record);
    void upsertBatch(List<AgentCommissionDaily> records);
    List<AgentCommissionDaily> searchByAgent(String agentCode, String fromDate, String toDate, int page, int limit);
    long countByAgent(String agentCode, String fromDate, String toDate);
    List<AgentCommissionDaily> searchByUser(String agentCode, String userNickname, String fromDate, String toDate, int page, int limit);
    long countByUser(String agentCode, String userNickname, String fromDate, String toDate);
}
