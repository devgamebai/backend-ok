package com.sunwinkr.minigame.api.controller;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-unit tests for AdminController static helpers.
 * No Spring context needed.
 *
 * Plan §3 / spec §4.
 */
class AdminControllerUnitTest {

    @RepeatedTest(20)
    void generateForSide_side0_sumLe10() {
        short[] dice = AdminController.generateForSide(0);
        int total = dice[0] + dice[1] + dice[2];
        assertThat(total).isLessThanOrEqualTo(10);
    }

    @RepeatedTest(20)
    void generateForSide_side1_sumGt10() {
        short[] dice = AdminController.generateForSide(1);
        int total = dice[0] + dice[1] + dice[2];
        assertThat(total).isGreaterThan(10);
    }

    @Test
    void generateForSide_allDiceInRange1To6() {
        for (int i = 0; i < 50; i++) {
            short[] dice = AdminController.generateForSide(i % 2);
            for (short d : dice) {
                assertThat(d).isBetween((short) 1, (short) 6);
            }
        }
    }
}
