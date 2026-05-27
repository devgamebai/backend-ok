package com.sunwinkr.lottery.api.dto;

import com.sunwinkr.lottery.engine.clock.LotteryClock;
import com.sunwinkr.lottery.engine.model.LotteryTicket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wire DTO for {@code GET /api/v2/lottery/xsmb/history}.
 *
 * <p>Player-facing pagination + per-ticket entries. Dates are emitted
 * as ISO-8601 strings with the {@code +07:00} Vietnam offset (FE Cocos
 * 2.4.4 parses these natively as {@code Date}); zone-less / UTC-looking
 * strings were rejected by FE.
 */
public final class HistoryDto {

    public List<Entry> tickets;
    /** Total rows matching the filter — for FE pagination counter. */
    public long total;
    /** Effective page size used by the server (after cap). */
    public int limit;
    /** Zero-based offset of the first row in {@link #tickets}. */
    public int offset;
    /** {@code true} if {@code offset + tickets.size() < total}. */
    public boolean hasMore;

    public HistoryDto() {
        this.tickets = new ArrayList<>();
    }

    public HistoryDto(List<Entry> tickets) {
        this.tickets = tickets == null ? new ArrayList<Entry>() : tickets;
    }

    public HistoryDto(List<Entry> tickets, long total, int offset, int limit) {
        this.tickets = tickets == null ? new ArrayList<Entry>() : tickets;
        this.total = total;
        this.offset = offset;
        this.limit = limit;
        this.hasMore = (long) offset + this.tickets.size() < total;
    }

    /** Per-ticket wire entry. */
    public static final class Entry {
        public Long ticketId;
        public String nickname;
        public int modeId;
        public String ticket;
        public long betValue;
        public Long prize;
        /** ISO-8601 with {@code +07:00} offset (Vietnam wall time). */
        public String createdDate;
        /** ISO-8601 with {@code +07:00} offset, or {@code null} if not settled. */
        public String settledAt;

        public Entry() {
        }

        public static Entry fromTicket(LotteryTicket t) {
            Entry e = new Entry();
            if (t == null) return e;
            e.ticketId = t.getTicketId();
            e.nickname = t.getNickname();
            e.modeId = t.getModeId();
            e.ticket = t.getTicket();
            // FE wire contract (Zeus 2026-05-18): expose the player-input
            // điểm count, NOT the stored stake. `lode.bet_value` stores the
            // post-rate amount (điểm × rate); `lode.bet_unit` keeps the raw
            // điểm. The UI label is "betValue" but the value is in điểm so
            // FE renders "2 điểm × rate × 1000 = 44.000 won" client-side.
            e.betValue = t.getBetUnit();
            e.prize = t.getPrize();
            e.createdDate = toVnIso(t.getCreatedDate());
            e.settledAt = toVnIso(t.getSettledAt());
            return e;
        }

        /**
         * Render a stored {@link LocalDateTime} (BetAcceptor stamps these
         * with {@code LocalDateTime.now(clock.withZone(LotteryClock.VN))},
         * so the wall value already IS Vietnam time) as ISO-8601 with the
         * {@code +07:00} offset so Cocos / browsers can parse it as a
         * proper {@code Date} without timezone ambiguity.
         */
        private static String toVnIso(LocalDateTime dt) {
            if (dt == null) return null;
            return dt.atZone(LotteryClock.VN).toOffsetDateTime().toString();
        }
    }
}
