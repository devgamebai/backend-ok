package com.sunwinkr.lottery.api.dto;

import com.sunwinkr.lottery.engine.model.LotteryResult;

import java.util.Collections;
import java.util.List;

/**
 * Wire DTO for {@code GET /api/v2/lottery/xsmb/result/{date}} +
 * {@code /results}. Plan §5.2.
 *
 * <p>Mirrors the {@link LotteryResult.Results} JSON shape but spells
 * "DB" without the Vietnamese diacritic for wire compatibility with
 * non-Unicode-safe clients. Internal {@link LotteryResult} keeps the
 * literal {@code ĐB} field (quirk #8).
 */
public final class ResultDto {

    public String time;
    public int countNumbers;

    public List<String> DB;
    public List<String> G1;
    public List<String> G2;
    public List<String> G3;
    public List<String> G4;
    public List<String> G5;
    public List<String> G6;
    public List<String> G7;

    public ResultDto() {
    }

    /** Build a wire DTO from the engine {@link LotteryResult}. */
    public static ResultDto fromResult(LotteryResult r) {
        ResultDto d = new ResultDto();
        if (r == null) {
            return d;
        }
        d.time = r.getTime();
        d.countNumbers = r.getCountNumbers();
        LotteryResult.Results res = r.getResults();
        if (res != null) {
            d.DB = safe(res.getĐB());
            d.G1 = safe(res.getG1());
            d.G2 = safe(res.getG2());
            d.G3 = safe(res.getG3());
            d.G4 = safe(res.getG4());
            d.G5 = safe(res.getG5());
            d.G6 = safe(res.getG6());
            d.G7 = safe(res.getG7());
        }
        return d;
    }

    private static List<String> safe(List<String> xs) {
        return xs == null ? Collections.<String>emptyList() : xs;
    }
}
