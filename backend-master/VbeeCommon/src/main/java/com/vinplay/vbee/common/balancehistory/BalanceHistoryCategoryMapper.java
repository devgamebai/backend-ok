package com.vinplay.vbee.common.balancehistory;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Maps a raw log_money_user_vin row to a BalanceHistoryCategory, and a set of categories
 * to a list of MongoDB regex patterns (applied as $or over action_name / service_name).
 *
 * Read-time only — no DB schema change.
 */
public final class BalanceHistoryCategoryMapper {

    private BalanceHistoryCategoryMapper() {}

    // ── Service-name buckets (exact, case-insensitive) ────────────────────────
    private static final Set<String> NAP_SERVICES = new HashSet<>(Arrays.asList(
            "atm", "ibanking", "momo", "zalopay", "the_cao",
            "nap_bank", "nap_atm", "nap_momo", "nap_zalopay", "nap_the_cao"));
    private static final Set<String> RUT_SERVICES = new HashSet<>(Arrays.asList(
            "rut_bank", "rut_atm", "rut_momo", "rut_zalopay"));
    private static final String ADMIN_ADJUST_SERVICE = "admin_adjust";

    // ── Action-name substring rules (case-insensitive). Order matters. ────────
    private static final Pattern P_RUT       = Pattern.compile("(?i)\\brut\\b|rút|rut tien|rút tiền");
    private static final Pattern P_CASHBACK  = Pattern.compile("(?i)hoàn cược|hoan cuoc|cashback|hoàn trả|hoan tra");
    private static final Pattern P_XFER_IN   = Pattern.compile("(?i)chuyển từ ví đại lý|chuyen tu vi dai ly|nhận từ đại lý|nhan tu dai ly|nhận từ ví phụ|nhan tu vi phu");
    private static final Pattern P_XFER_OUT  = Pattern.compile("(?i)chuyển sang ví đại lý|chuyen sang vi dai ly|chuyển sang ví phụ|chuyen sang vi phu|chuyển ra|chuyen ra");
    private static final Pattern P_GIFTCODE  = Pattern.compile("(?i)giftcode|gift\\s*code|đổi giftcode|doi giftcode");
    private static final Pattern P_BET       = Pattern.compile("(?i)cược|cuoc");
    private static final Pattern P_WIN       = Pattern.compile("(?i)thắng|thang|nhận thưởng|nhan thuong|trúng|trung|hòa|hoa");
    private static final Pattern P_ADMIN_ADJ = Pattern.compile("(?i)admin cộng|admin cong|admin trừ|admin tru");

    /**
     * Classify a single row.
     */
    public static BalanceHistoryCategory classify(String actionName, String serviceName, long moneyExchange) {
        String a = actionName == null ? "" : actionName;
        String s = serviceName == null ? "" : serviceName.toLowerCase();

        // 1. Service-name exact buckets
        if (NAP_SERVICES.contains(s) || s.startsWith("nap_")) return BalanceHistoryCategory.NAP;
        if (RUT_SERVICES.contains(s) || s.startsWith("rut_")) return BalanceHistoryCategory.RUT;
        if (ADMIN_ADJUST_SERVICE.equals(s))                   return BalanceHistoryCategory.ADMIN_ADJUST;

        // 2. Action-name regex
        if (P_CASHBACK.matcher(a).find())   return BalanceHistoryCategory.CASHBACK;
        if (P_XFER_IN.matcher(a).find())    return BalanceHistoryCategory.TRANSFER_IN;
        if (P_XFER_OUT.matcher(a).find())   return BalanceHistoryCategory.TRANSFER_OUT;
        if (P_GIFTCODE.matcher(a).find())   return BalanceHistoryCategory.GIFTCODE;
        if (P_ADMIN_ADJ.matcher(a).find())  return BalanceHistoryCategory.ADMIN_ADJUST;
        if (P_RUT.matcher(a).find())        return BalanceHistoryCategory.RUT;
        if (P_BET.matcher(a).find()  && moneyExchange < 0)  return BalanceHistoryCategory.GAME_BET;
        if (P_WIN.matcher(a).find()  && moneyExchange >= 0) return BalanceHistoryCategory.GAME_WIN;

        return BalanceHistoryCategory.OTHER;
    }

    /**
     * Wire CSV (e.g. "nap,rut") → set of categories. Returns null if the CSV
     * contains an unknown token (caller maps to errorCode 400). Empty / null
     * input returns all categories (= no filter).
     */
    public static Set<BalanceHistoryCategory> parseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return EnumSet.allOf(BalanceHistoryCategory.class);
        }
        Set<BalanceHistoryCategory> out = EnumSet.noneOf(BalanceHistoryCategory.class);
        for (String tok : csv.split(",")) {
            BalanceHistoryCategory c = BalanceHistoryCategory.fromWire(tok);
            if (c == null) return null;
            out.add(c);
        }
        return out;
    }

    /**
     * Returns the regex source strings used to filter action_name for a given category.
     * The caller wraps each with new BasicDBObject("$regex", source).append("$options","i").
     * Empty list means "match anything" (used for OTHER — handled by negation at query layer).
     */
    public static List<String> actionNameRegexFor(BalanceHistoryCategory c) {
        switch (c) {
            case RUT:          return Collections.singletonList("rut|rút");
            case CASHBACK:     return Arrays.asList("hoàn cược", "hoan cuoc", "cashback", "hoàn trả", "hoan tra");
            case TRANSFER_IN:  return Arrays.asList("chuyển từ ví đại lý", "chuyen tu vi dai ly", "nhận từ đại lý", "nhan tu dai ly", "nhận từ ví phụ", "nhan tu vi phu");
            case TRANSFER_OUT: return Arrays.asList("chuyển sang ví đại lý", "chuyen sang vi dai ly", "chuyển sang ví phụ", "chuyen sang vi phu", "chuyển ra", "chuyen ra");
            case GIFTCODE:     return Arrays.asList("giftcode", "gift code", "đổi giftcode", "doi giftcode");
            case GAME_BET:     return Arrays.asList("cược", "cuoc");
            case GAME_WIN:     return Arrays.asList("thắng", "thang", "nhận thưởng", "nhan thuong", "trúng", "trung", "hòa", "hoa");
            case ADMIN_ADJUST: return Arrays.asList("admin cộng", "admin cong", "admin trừ", "admin tru");
            default:           return Collections.emptyList();
        }
    }

    /**
     * Returns the service_name values used to filter for a given category.
     */
    public static Set<String> serviceNamesFor(BalanceHistoryCategory c) {
        switch (c) {
            case NAP:          return new LinkedHashSet<>(NAP_SERVICES);
            case RUT:          return new LinkedHashSet<>(RUT_SERVICES);
            case ADMIN_ADJUST: return new LinkedHashSet<>(Collections.singleton(ADMIN_ADJUST_SERVICE));
            default:           return Collections.emptySet();
        }
    }
}
