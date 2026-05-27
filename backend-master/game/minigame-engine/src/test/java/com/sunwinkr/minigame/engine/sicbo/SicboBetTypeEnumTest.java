package com.sunwinkr.minigame.engine.sicbo;

import com.sunwinkr.minigame.engine.sicbo.bet.SicboBetType;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Validates that SicboBetType has exactly 52 entries matching PotSicbo (PS:7-58),
 * and that the isOneDiceSpecial flag is set correctly.
 */
public class SicboBetTypeEnumTest {

    @Test
    public void allFiftyTwoEntriesPresent() {
        SicboBetType[] values = SicboBetType.values();
        assertEquals("Must have exactly 52 entries", 52, values.length);

        // All IDs 1..52 must be present exactly once
        Set<Integer> ids = new HashSet<Integer>();
        for (SicboBetType t : values) {
            assertTrue("Duplicate id " + t.getId(), ids.add(t.getId()));
        }
        for (int id = 1; id <= 52; id++) {
            assertTrue("Missing id " + id, ids.contains(id));
        }

        // Verify every name is non-null and non-empty
        Set<String> names = new HashSet<String>();
        for (SicboBetType t : values) {
            assertNotNull("Name must not be null for id " + t.getId(), t.getName());
            assertFalse("Name must not be empty for id " + t.getId(), t.getName().isEmpty());
            assertTrue("Duplicate name " + t.getName(), names.add(t.getName()));
        }
    }

    @Test
    public void idToNameMappingMatchesPotSicbo() {
        // Spot-check every group against PotSicbo declaration (PS:7-58)
        assertEntry(1,  "POINT_4",   61);
        assertEntry(2,  "POINT_5",   31);
        assertEntry(3,  "POINT_6",   18);
        assertEntry(4,  "POINT_7",   13);
        assertEntry(5,  "POINT_8",    9);
        assertEntry(6,  "POINT_9",    7);
        assertEntry(7,  "POINT_10",   7);
        assertEntry(8,  "POINT_11",   7);
        assertEntry(9,  "POINT_12",   7);
        assertEntry(10, "POINT_13",   9);
        assertEntry(11, "POINT_14",  13);
        assertEntry(12, "POINT_15",  18);
        assertEntry(13, "POINT_16",  31);
        assertEntry(14, "POINT_17",  61);

        assertEntry(15, "ONE_DICE_1", 1);
        assertEntry(16, "ONE_DICE_2", 1);
        assertEntry(17, "ONE_DICE_3", 1);
        assertEntry(18, "ONE_DICE_4", 1);
        assertEntry(19, "ONE_DICE_5", 1);
        assertEntry(20, "ONE_DICE_6", 1);

        assertEntry(21, "DOUBLE_DICES_1_1", 6);
        assertEntry(22, "DOUBLE_DICES_1_2", 6);
        assertEntry(27, "DOUBLE_DICES_2_2", 6);
        assertEntry(32, "DOUBLE_DICES_3_3", 6);
        assertEntry(36, "DOUBLE_DICES_4_4", 6);
        assertEntry(39, "DOUBLE_DICES_5_5", 6);
        assertEntry(41, "DOUBLE_DICES_6_6", 6);

        assertEntry(42, "TRIPLE_DICES_1", 31);
        assertEntry(43, "TRIPLE_DICES_2", 31);
        assertEntry(44, "TRIPLE_DICES_3", 31);
        assertEntry(45, "TRIPLE_DICES_4", 31);
        assertEntry(46, "TRIPLE_DICES_5", 31);
        assertEntry(47, "TRIPLE_DICES_6", 31);

        assertEntry(48, "TAI",              2);
        assertEntry(49, "XIU",              2);
        assertEntry(50, "CHAN",             2);
        assertEntry(51, "LE",               2);
        assertEntry(52, "ANY_TRIPLE_DICES", 31);
    }

    @Test
    public void oneDiceSpecialFlag() {
        // IDs 15-20 must have isOneDiceSpecial == true
        for (int id = 15; id <= 20; id++) {
            SicboBetType t = SicboBetType.byId(id);
            assertTrue("id=" + id + " (" + t.getName() + ") must have isOneDiceSpecial=true",
                    t.isOneDiceSpecial());
        }

        // All other IDs must have isOneDiceSpecial == false
        for (int id = 1; id <= 52; id++) {
            if (id >= 15 && id <= 20) continue;
            SicboBetType t = SicboBetType.byId(id);
            assertFalse("id=" + id + " (" + t.getName() + ") must have isOneDiceSpecial=false",
                    t.isOneDiceSpecial());
        }
    }

    @Test
    public void byIdLookupMatchesByIdMap() {
        for (int id = 1; id <= 52; id++) {
            SicboBetType direct = SicboBetType.byId(id);
            SicboBetType viaMap = SicboBetType.BY_ID.get(id);
            assertSame("byId(" + id + ") and BY_ID map must return same instance",
                    direct, viaMap);
        }
    }

    @Test
    public void byNameLookupRoundTrips() {
        for (SicboBetType t : SicboBetType.values()) {
            assertSame("byName round-trip must return same instance for " + t.getName(),
                    t, SicboBetType.byName(t.getName()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void byIdThrowsForUnknownId() {
        SicboBetType.byId(99);
    }

    @Test(expected = IllegalArgumentException.class)
    public void byNameThrowsForUnknownName() {
        SicboBetType.byName("NO_SUCH_BET");
    }

    // -----------------------------------------------------------------------

    private static void assertEntry(int expectedId, String expectedName, int expectedRotation) {
        SicboBetType t = SicboBetType.byId(expectedId);
        assertEquals("name mismatch for id " + expectedId, expectedName, t.getName());
        assertEquals("rotation mismatch for id " + expectedId, expectedRotation, t.getRotation());
    }
}
