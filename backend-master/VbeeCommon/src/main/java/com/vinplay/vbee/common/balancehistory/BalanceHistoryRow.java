package com.vinplay.vbee.common.balancehistory;

import com.vinplay.vbee.common.response.BalanceGuard;
import org.json.JSONObject;

public class BalanceHistoryRow {
    public long transId;
    public String transTime;
    public String nickname;
    public String category;       // BalanceHistoryCategory.wire()
    public String actionLabel;    // raw action_name
    public String serviceName;
    public long amount;           // signed money_exchange
    public long balanceBefore;
    public long balanceAfter;
    public long fee;              // fee column from log_money_user_vin (0 for most actions)
    public String description;

    public JSONObject toJson() {
        JSONObject j = new JSONObject();
        j.put("trans_id", transId);
        j.put("trans_time", transTime != null ? transTime : "");
        j.put("nickname", nickname != null ? nickname : "");
        j.put("category", category != null ? category : "other");
        j.put("action_label", actionLabel != null ? actionLabel : "");
        j.put("service_name", serviceName != null ? serviceName : "");
        j.put("amount", amount);
        // Clamp balance fields per CLAUDE.md: log_money_user_vin.current_money is
        // populated from vinTotal (cumulative P&L). For a losing player vinTotal can
        // go negative, but a "balance" wire field should never ship negative — that
        // is the class of bug BalanceGuard exists to catch (SUN-748 / SUN-753).
        // Arithmetic invariant `balance_after = balance_before + amount` therefore
        // only holds when both clamped values are positive; tests should account
        // for this on legacy rows.
        j.put("balance_before", BalanceGuard.clamp(balanceBefore, "BalanceHistoryRow.balance_before"));
        j.put("balance_after",  BalanceGuard.clamp(balanceAfter,  "BalanceHistoryRow.balance_after"));
        j.put("fee", fee);
        j.put("description", description != null ? description : "");
        return j;
    }
}
