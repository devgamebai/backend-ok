package com.sunwinkr.lottery.engine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable XSMB draw payload. Gson-compatible (field-direct mapping;
 * default constructor reserved by Gson via reflection, no need to wire).
 *
 * <p>Ported from {@code game.modules.minigame.model.LotteryResult}. Two
 * deliberate quirks preserved (lottery-rules-spec.md §8):
 * <ul>
 *   <li><b>Field {@code ĐB}</b> — literal Unicode field name. Renaming via
 *       reflection breaks JSON parsing (quirk #8). Field annotated NOT
 *       with Gson {@code @SerializedName} because the JSON key matches
 *       the field name byte-for-byte.</li>
 *   <li><b>Mutable list refs.</b> Gson assigns fields directly — making
 *       them {@code final} requires custom deserialization. To preserve
 *       Gson compatibility while protecting callers, the public list
 *       accessors return {@link Collections#unmodifiableList} wrappers.</li>
 * </ul>
 *
 * <p>{@link Results#get27()} and {@link Results#get24()} return the
 * 27-line / 24-line prize pools used by Modes 1, 3, 4, 5 (27) and
 * Mode 2 (24).
 */
public final class LotteryResult {

    private int countNumbers;
    private String time;
    private Results results;

    public int getCountNumbers() {
        return countNumbers;
    }

    public String getTime() {
        return time;
    }

    public Results getResults() {
        return results;
    }

    /** Gson deserializer entry point. Production callers should not invoke. */
    public void setCountNumbers(int countNumbers) {
        this.countNumbers = countNumbers;
    }

    /** Gson deserializer entry point. Production callers should not invoke. */
    public void setTime(String time) {
        this.time = time;
    }

    /** Gson deserializer entry point. Production callers should not invoke. */
    public void setResults(Results results) {
        this.results = results;
    }

    /** Inner JSON object — preserves the literal {@code ĐB} field name. */
    public static final class Results {

        // CRITICAL: do NOT rename. Quirk #8 — literal Unicode field name
        // matches the upstream JSON key ("ĐB"). Reflection-based rename
        // breaks Gson deserialization.
        private List<String> ĐB;
        private List<String> G1;
        private List<String> G2;
        private List<String> G3;
        private List<String> G4;
        private List<String> G5;
        private List<String> G6;
        private List<String> G7;

        public List<String> getĐB() {
            return safe(ĐB);
        }

        public List<String> getG1() {
            return safe(G1);
        }

        public List<String> getG2() {
            return safe(G2);
        }

        public List<String> getG3() {
            return safe(G3);
        }

        public List<String> getG4() {
            return safe(G4);
        }

        public List<String> getG5() {
            return safe(G5);
        }

        public List<String> getG6() {
            return safe(G6);
        }

        public List<String> getG7() {
            return safe(G7);
        }

        public void setĐB(List<String> v) {
            this.ĐB = v;
        }

        public void setG1(List<String> v) {
            this.G1 = v;
        }

        public void setG2(List<String> v) {
            this.G2 = v;
        }

        public void setG3(List<String> v) {
            this.G3 = v;
        }

        public void setG4(List<String> v) {
            this.G4 = v;
        }

        public void setG5(List<String> v) {
            this.G5 = v;
        }

        public void setG6(List<String> v) {
            this.G6 = v;
        }

        public void setG7(List<String> v) {
            this.G7 = v;
        }

        /**
         * @return 27-line prize pool: ĐB ∪ G1..G7. Used by Modes 1, 3, 4, 5.
         */
        public List<String> get27() {
            ArrayList<String> all = new ArrayList<>();
            addAll(all, ĐB);
            addAll(all, G1);
            addAll(all, G2);
            addAll(all, G3);
            addAll(all, G4);
            addAll(all, G5);
            addAll(all, G6);
            addAll(all, G7);
            return Collections.unmodifiableList(all);
        }

        /**
         * @return 24-line prize pool: ĐB ∪ G1..G6 (excl G7). Used by Mode 2.
         */
        public List<String> get24() {
            ArrayList<String> all = new ArrayList<>();
            addAll(all, ĐB);
            addAll(all, G1);
            addAll(all, G2);
            addAll(all, G3);
            addAll(all, G4);
            addAll(all, G5);
            addAll(all, G6);
            return Collections.unmodifiableList(all);
        }

        private static void addAll(List<String> sink, List<String> src) {
            if (src != null) {
                sink.addAll(src);
            }
        }

        private static List<String> safe(List<String> src) {
            return src == null ? Collections.<String>emptyList() : Collections.unmodifiableList(src);
        }
    }
}
