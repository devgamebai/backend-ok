package com.sunwinkr.minigame.engine.bet;

import com.sunwinkr.minigame.engine.port.BetRecorder;

import java.util.ArrayList;
import java.util.List;

/** Test-only {@link BetRecorder} that captures every recorded bet. */
final class RecordingBetRecorder implements BetRecorder {

    final List<BetRecord> records = new ArrayList<>();

    @Override
    public void recordBet(BetRecord record) {
        records.add(record);
    }
}
