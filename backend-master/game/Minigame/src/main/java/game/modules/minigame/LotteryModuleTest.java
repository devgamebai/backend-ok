/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  org.junit.Assert
 *  org.junit.Before
 *  org.junit.Test
 */
package game.modules.minigame;

import com.google.gson.Gson;
import game.modules.minigame.LotteryModule;
import game.modules.minigame.model.LotteryMode;
import game.modules.minigame.model.LotteryResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LotteryModuleTest {
    @Before
    public void setUp() {
    }

    @Test
    public void testGetPrize() {
        String json = "{\"countNumbers\":27,\"time\":\"1-4-2024\",\"results\":{\"\u0110B\":[\"19052\"],\"G1\":[\"64293\"],\"G2\":[\"66910\",\"37980\"],\"G3\":[\"03154\",\"05297\",\"37583\",\"24357\",\"50612\",\"56159\"],\"G4\":[\"1490\",\"6212\",\"7679\",\"2105\"],\"G5\":[\"4438\",\"2763\",\"4042\",\"1066\",\"7302\",\"1099\"],\"G6\":[\"559\",\"345\",\"633\"],\"G7\":[\"09\",\"93\",\"06\",\"38\"]}}";
        Gson gson = new Gson();
        LotteryResult lotteryResult = (LotteryResult)gson.fromJson(json, LotteryResult.class);
        long betValue = 100L;
        long expectedPrize = 99L * betValue / (long)LotteryMode.LO_2_SO.getRate();
        long actualPrize = LotteryModule.getPrize(lotteryResult, 1L, betValue, "09");
        Assert.assertEquals((long)expectedPrize, (long)actualPrize);
        expectedPrize = betValue * 17L;
        actualPrize = LotteryModule.getPrize(lotteryResult, 3L, betValue, "09,10");
        Assert.assertEquals((long)expectedPrize, (long)actualPrize);
        actualPrize = LotteryModule.getPrize(lotteryResult, 3L, betValue, "34,11");
        Assert.assertEquals((long)0L, (long)actualPrize);
        actualPrize = LotteryModule.getPrize(lotteryResult, 3L, betValue, "32,11");
        Assert.assertEquals((long)0L, (long)actualPrize);
        actualPrize = LotteryModule.getPrize(lotteryResult, 3L, betValue, "01,02");
        Assert.assertEquals((long)0L, (long)actualPrize);
        actualPrize = LotteryModule.getPrize(lotteryResult, 3L, betValue, "34");
        Assert.assertEquals((long)0L, (long)actualPrize);
    }
}

