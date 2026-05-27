package com.sunwinkr.lottery.engine.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LotteryModeTest {

    @Test
    void allElevenModesPresent() {
        // Verify all 11 modes per the 2026-05-15 rate-table refresh
        // (docs/LOTTERY_LODE.md / docs/ref/LodeRatio). IDs 1..11 contiguous.
        assertMode(1, "BAO_LO_2_SO", 22, 80);
        assertMode(2, "BAO_LO_3_SO", 23, 600);
        assertMode(3, "XIEN_2", 1, 12);
        assertMode(4, "XIEN_3", 1, 48);
        assertMode(5, "XIEN_4", 1, 160);
        assertMode(6, "DE_GIAI_NHAT", 1, 85);
        assertMode(7, "DE_DAC_BIET", 1, 85);
        assertMode(8, "BA_CANG_DAC_BIET", 1, 450);
        assertMode(9, "LO_TRUOT_XIEN_10", 1, 10);
        assertMode(10, "LO_TRUOT_XIEN_12", 1, 16);
        assertMode(11, "LO_TRUOT_XIEN_14", 1, 20);

        // Cardinality + ID gaps.
        assertThat(LotteryMode.values()).hasSize(11);
        assertThat(LotteryMode.byId(0)).isEmpty();
        assertThat(LotteryMode.byId(12)).isEmpty();
        assertThat(LotteryMode.byId(99)).isEmpty();
    }

    @Test
    void byIdLookupReturnsCorrectMode() {
        // Optional contract — caller MUST handle empty (legacy returned null
        // and exploded; we surface it as Optional.empty()).
        Optional<LotteryMode> m = LotteryMode.byId(7);
        assertThat(m).isPresent();
        assertThat(m.get()).isEqualTo(LotteryMode.DE_DAC_BIET);
        assertThat(LotteryMode.byId(-1)).isEmpty();
    }

    @Test
    void settersRemoved() {
        // Audit hardening: mutable setters on a JVM-singleton enum are a
        // latent leak surface (audit §5 — LotteryMode setters). Reflection
        // check enforces they cannot be reintroduced silently.
        String[] forbidden = {
                "setRate",
                "setPrizeMultiplier",
                "setId",
                "setName",
                "setDescription"
        };
        for (String name : forbidden) {
            boolean present = Arrays.stream(LotteryMode.class.getDeclaredMethods())
                    .map(Method::getName)
                    .anyMatch(name::equals);
            assertThat(present)
                    .as("LotteryMode must not expose %s — see audit §5 anti-cheat", name)
                    .isFalse();
        }
    }

    private static void assertMode(int id, String enumName, int rate, int prizeMul) {
        Optional<LotteryMode> opt = LotteryMode.byId(id);
        assertThat(opt).as("mode id=%d", id).isPresent();
        LotteryMode m = opt.get();
        assertThat(m.name()).isEqualTo(enumName);
        assertThat(m.getId()).isEqualTo(id);
        assertThat(m.getRate()).as("rate for id=%d", id).isEqualTo(rate);
        assertThat(m.getPrizeMultiplier()).as("prizeMul for id=%d", id).isEqualTo(prizeMul);
    }
}
