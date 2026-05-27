package game.GameConfig.BotJackpotConfig;

import java.util.Random;

public class SlotChiemtinhBotConfig {
    public long betMin = 0;
    public long betMax = 0;
    public int numBot = 0;

    // Money added to jackpot per 5-second tick, scaled by room tier
    private static final int[] JACKPOT_PER_5S = {100, 1000, 10000};
    // Time range (in seconds) for bot to eat jackpot, scaled by room tier
    private static final int[][] TIME_BOT_EAT_RANGE = {
        {300, 600},    // room 0: 5-10 min
        {600, 1200},   // room 1: 10-20 min
        {1200, 2400}   // room 2: 20-40 min
    };

    private static final Random random = new Random();

    /**
     * Returns random money to add to jackpot per 5-second tick for the given room tier.
     * @param roomTier 0=100, 1=1000, 2=10000
     */
    public int randomJackPotPer5s(int roomTier) {
        if (roomTier < 0 || roomTier >= JACKPOT_PER_5S.length) {
            return 0;
        }
        int base = JACKPOT_PER_5S[roomTier];
        return base + random.nextInt(base + 1);
    }

    /**
     * Returns random time (in seconds) until next bot eats jackpot for the given room tier.
     * @param roomTier 0=100, 1=1000, 2=10000
     */
    public int randomTimeBotEat(int roomTier) {
        if (roomTier < 0 || roomTier >= TIME_BOT_EAT_RANGE.length) {
            return 600;
        }
        int min = TIME_BOT_EAT_RANGE[roomTier][0];
        int max = TIME_BOT_EAT_RANGE[roomTier][1];
        return min + random.nextInt(max - min + 1);
    }
}
