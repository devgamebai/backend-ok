package com.sunwinkr.minigame.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wire DTO for {@code GET /api/v2/taixiu/history}. Returns the last
 * {@code n} round results capped at 120 (plan §2.1 L8).
 */
public final class HistoryDto {

    public final List<HistoryEntry> entries;
    public final int count;

    public HistoryDto(List<HistoryEntry> entries) {
        this.entries = entries == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(entries));
        this.count = this.entries.size();
    }

    public static final class HistoryEntry {
        public long referenceId;
        public short result;
        public short dice1;
        public short dice2;
        public short dice3;
        public long ts;

        public HistoryEntry() {
        }
    }
}
