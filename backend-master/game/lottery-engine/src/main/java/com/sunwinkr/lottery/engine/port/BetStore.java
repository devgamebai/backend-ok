package com.sunwinkr.lottery.engine.port;

import com.sunwinkr.lottery.engine.model.LotteryTicket;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@code vinplay_minigame.lode} (per-ticket bet rows).
 *
 * <p>Adapter ships in PR-3 (Spring + JDBC) and wraps {@code LoDeDaoImpl}.
 *
 * <p><b>SECURITY — H3 fix.</b> The legacy {@code LoDeDaoImpl.search} /
 * {@code count} build SQL via string concatenation, opening a clear SQLi
 * surface (see {@code docs/specs/lottery-anticheat-audit.md} +
 * {@code docs/plans/lottery-extraction-plan.md §2.6 H3}). The
 * {@link #search} / {@link #count} methods on THIS port accept strongly
 * typed criteria — the PR-3 adapter MUST use {@link java.sql.PreparedStatement}
 * for every binding (nickName, ticket, modeId, timeStart, timeEnd). The
 * port signature alone defeats injection — the adapter can never receive a
 * pre-baked SQL string.
 */
public interface BetStore {

    /**
     * Persist a fresh ticket. Sets {@code prize=NULL} and
     * {@code settled_at=NULL} so the row enters the pending settle window.
     *
     * @param ticket new ticket (ticketId field should be {@code null})
     * @return the same ticket with {@code ticketId} populated
     */
    LotteryTicket insert(LotteryTicket ticket);

    /**
     * Locate pending bets for a Vietnam-wall date.
     *
     * <p><b>TZ FIX.</b> Replaces the audit-flagged
     * {@code ZoneId.systemDefault()} usage in {@code LoDeDaoImpl} —
     * adapter MUST anchor the {@code created_date} window to
     * {@code LotteryClock.VN}. The legacy window
     * {@code (yesterday lockTime, today lockTime)} is preserved, but
     * computed in Hanoi wall clock — closes AMBIGUOUS #7.
     *
     * @param vnDate    Vietnam-wall draw date (i.e. today, post-scrape)
     * @param lockTime  bet cutoff wall time (i.e.
     *                  {@code LotteryClock.LOCK_TIME})
     * @return pending tickets, prize / settled_at both null
     */
    List<LotteryTicket> findPendingForDate(LocalDate vnDate, LocalTime lockTime);

    /**
     * Mark a ticket settled. Adapter writes {@code prize = ?},
     * {@code settled_at = NOW()}, and {@code settle_status = 'SETTLED'}
     * in a single UPDATE — closes audit finding L-1 (pre-settle result
     * reveal). {@code prize=0} is a valid settle-complete state (player lost).
     */
    void markSettled(long ticketId, long prize);

    /**
     * SUN-1339 B1: Look up a single ticket by its primary key.
     *
     * @param ticketId {@code lode.id}
     * @return the ticket, or empty if not found
     */
    java.util.Optional<LotteryTicket> findById(long ticketId);

    /**
     * SUN-1339 B2: Flip {@code settle_status} to {@code 'VOIDED'}.
     * Adapter performs a conditional UPDATE:
     * {@code WHERE id = ? AND settle_status = 'SETTLED'}. Returns the
     * number of rows updated (0 = row already voided or not settled;
     * 1 = success).
     *
     * @param ticketId {@code lode.id}
     * @return rows updated (1 on success, 0 if precondition not met)
     */
    int voidTicket(long ticketId);

    /**
     * History endpoint backing — return the user's recent tickets, newest
     * first. Paging carried by {@link Paging}.
     */
    List<LotteryTicket> findByUser(String nickname, Paging paging);

    /**
     * Total row count for the user's history — used by the player-facing
     * {@code /xsmb/history} endpoint to ship pagination metadata
     * ({@code total}, {@code hasMore}) alongside the page.
     */
    long countByUser(String nickname);

    /**
     * Admin search. Adapter MUST use PreparedStatement bindings — see
     * class-level SECURITY note + audit finding H3.
     */
    List<LotteryTicket> search(SearchFilter filter);

    /**
     * Admin search count for paging. Same SECURITY constraint as
     * {@link #search}.
     */
    long count(SearchFilter filter);

    /** Pure paging carrier — adapter maps to LIMIT/OFFSET. */
    final class Paging {
        public final int offset;
        public final int limit;

        public Paging(int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
        }
    }

    /**
     * Strongly typed admin filter. PreparedStatement bindings — no SQL
     * concat, no string interpolation. Any null field means "no filter
     * on this column".
     */
    final class SearchFilter {
        public final Optional<String> nickname;
        public final Optional<String> ticket;
        public final Optional<Integer> modeId;
        public final Optional<LocalDate> from;
        public final Optional<LocalDate> to;
        public final Paging paging;

        public SearchFilter(Optional<String> nickname,
                            Optional<String> ticket,
                            Optional<Integer> modeId,
                            Optional<LocalDate> from,
                            Optional<LocalDate> to,
                            Paging paging) {
            this.nickname = nickname;
            this.ticket = ticket;
            this.modeId = modeId;
            this.from = from;
            this.to = to;
            this.paging = paging;
        }
    }
}
