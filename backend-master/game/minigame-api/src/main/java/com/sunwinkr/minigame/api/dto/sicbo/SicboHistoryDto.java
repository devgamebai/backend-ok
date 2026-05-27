package com.sunwinkr.minigame.api.dto.sicbo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wire DTO for {@code GET /api/v2/sicbo/history}. Returns the last
 * {@code n} round results capped at 120 (plan §2.1 L8 analog).
 */
public final class SicboHistoryDto {

    public final List<SicboHistoryEntry> entries;
    public final int count;

    public SicboHistoryDto(List<SicboHistoryEntry> entries) {
        this.entries = entries == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(entries));
        this.count = this.entries.size();
    }

    public static final class SicboHistoryEntry {
        public long referenceId;
        public short dice1;
        public short dice2;
        public short dice3;
        public int total;
        /** Vietnamese label: "TAI" / "XIU". */
        public String taiXiu;
        public long ts;

        public SicboHistoryEntry() {
        }
    }
}
