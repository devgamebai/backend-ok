package com.sunwinkr.lottery.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire DTO for {@code GET /api/v2/lottery/admin/transactions}. Plan §5.2.
 *
 * <p>Pages a {@link HistoryDto.Entry} list with the total {@code count}
 * for client paging. Backed by
 * {@code BetStore.search} / {@code BetStore.count} which use
 * PreparedStatement bindings (closes H3 SQL injection).
 */
public final class SearchResultDto {

    public List<HistoryDto.Entry> rows;
    public long total;
    public int offset;
    public int limit;

    public SearchResultDto() {
        this.rows = new ArrayList<>();
    }

    public SearchResultDto(List<HistoryDto.Entry> rows, long total, int offset, int limit) {
        this.rows = rows == null ? new ArrayList<HistoryDto.Entry>() : rows;
        this.total = total;
        this.offset = offset;
        this.limit = limit;
    }
}
