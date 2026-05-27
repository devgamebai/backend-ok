package game.GameConfig.BotJackpotConfig;

import java.util.Random;

public class SlotSexyGirlBotConfig {
    public long betMin = 0;
    public long betMax = 0;
    public int numBot = 0;

    private static final int[] JACKPOT_PER_5S = {100, 1000, 10000};
    private static final int[][] TIME_BOT_EAT_RANGE = {
        {300, 600},
        {600, 1200},
        {1200, 2400}
    };
    private static final Random random = new Random();

    public int randomJackPotPer5s(int roomTier) {
        if (roomTier < 0 || roomTier >= JACKPOT_PER_5S.length) return 0;
        int base = JACKPOT_PER_5S[roomTier];
        return base + random.nextInt(base + 1);
    }

    public int randomTimeBotEat(int roomTier) {
        if (roomTier < 0 || roomTier >= TIME_BOT_EAT_RANGE.length) return 600;
        int min = TIME_BOT_EAT_RANGE[roomTier][0];
        int max = TIME_BOT_EAT_RANGE[roomTier][1];
        return min + random.nextInt(max - min + 1);
    }
}

