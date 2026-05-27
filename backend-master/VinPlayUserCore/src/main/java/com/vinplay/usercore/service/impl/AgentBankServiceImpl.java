/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.dal.dao.AgentDAO;
import com.vinplay.dal.dao.impl.AgentDAOImpl;
import com.vinplay.usercore.dao.AgentBankDao;
import com.vinplay.usercore.dao.impl.AgentBankDaoImpl;
import com.vinplay.usercore.entities.AgentBank;
import com.vinplay.usercore.service.AgentBankService;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class AgentBankServiceImpl
implements AgentBankService {
    private static final Logger logger = Logger.getLogger((String)"user_core");
    private AgentBankDao dao = new AgentBankDaoImpl();
    private AgentDAO agentDao = new AgentDAOImpl();

    private String ValidateConfig(AgentBank agentBank) {
        try {
            if (StringUtils.isBlank((CharSequence)agentBank.getAgent_code())) {
                return "Agent code can not empty";
            }
            if (StringUtils.isBlank((CharSequence)agentBank.getBank_acount())) {
                return "Bank account can not empty";
            }
            if (StringUtils.isBlank((CharSequence)agentBank.getBank_code())) {
                return "Bank code can not empty";
            }
            if (StringUtils.isBlank((CharSequence)agentBank.getBank_number())) {
                return "Bank number can not empty";
            }
            if (this.agentDao.DetailUserAgentByCode(agentBank.getAgent_code()) == null) {
                return "Agent code is invalid";
            }
            if (agentBank.getId() == 0L) {
                if (this.dao.getByBankNumber(agentBank.getBank_number()) != null) {
                    return "Bank number is exist";
                }
                if (this.dao.getByBankCode(agentBank.getAgent_code(), agentBank.getBank_code()) != null) {
                    return "Bank code is exist";
                }
            } else {
                AgentBank oldAgentBank = this.dao.getByBankNumber(agentBank.getBank_number());
                if (oldAgentBank != null && oldAgentBank.getId() != agentBank.getId()) {
                    return "Bank number is exist";
                }
                oldAgentBank = this.dao.getByBankCode(agentBank.getAgent_code(), agentBank.getBank_code());
                if (oldAgentBank != null && oldAgentBank.getId() != agentBank.getId()) {
                    return "Bank code is exist";
                }
            }
            return "success";
        }
        catch (Exception e) {
            logger.error(("Error ValidateConfig: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public String create(AgentBank agentBank) {
        try {
            String result = "";
            result = this.ValidateConfig(agentBank);
            if (!"success".equals(result)) {
                return result;
            }
            return this.dao.create(agentBank) ? "success" : "Insert not success";
        }
        catch (Exception e) {
            logger.error(("Error create bank of agent: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public AgentBank getById(long id) {
        try {
            return this.dao.getById(id);
        }
        catch (Exception e) {
            logger.error(("GetById exception: " + e.getMessage()));
            return null;
        }
    }

    @Override
    public AgentBank getByBankNumber(String bankNumber) {
        try {
            return this.dao.getByBankNumber(bankNumber);
        }
        catch (Exception e) {
            logger.error(("GetByBankNumber exception: " + e.getMessage()));
            return null;
        }
    }

    @Override
    public AgentBank getByBankCode(String agentCode, String bankCode) {
        try {
            return this.dao.getByBankCode(agentCode, bankCode);
        }
        catch (Exception e) {
            logger.error(("GetByBankNumber exception: " + e.getMessage()));
            return null;
        }
    }

    @Override
    public String update(AgentBank agentBank) {
        try {
            if (agentBank.getId() < 1L) {
                return "Id is invalid";
            }
            String result = "";
            result = this.ValidateConfig(agentBank);
            if (!"success".equals(result)) {
                return result;
            }
            return this.dao.update(agentBank) ? "success" : "Update not success";
        }
        catch (Exception e) {
            logger.error(("Error update bank of agent: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public String Delete(long id) {
        try {
            if (id < 1L) {
                return "Id is invalid";
            }
            return this.dao.delete(id) ? "success" : "Delete not success";
        }
        catch (Exception e) {
            logger.error(("Error delete bank of agent: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public Map<String, Object> search(String keyword, String agentCode, int pageIndex, int limit) {
        try {
            return this.dao.search(keyword, agentCode, pageIndex, limit);
        }
        catch (Exception e) {
            logger.error(("Error delete bank of agent: " + e.getMessage()));
            return new HashMap<String, Object>();
        }
    }
}

