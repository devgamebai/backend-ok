package game.modules.SlotDescription;

import com.google.gson.Gson;

public class SlotDescriptionUtils {
    public static Gson gson = new Gson();

    public static String getBetDescription(String gameID){
        return gson.toJson(new BetDescription(gameID));
    }

    public static String getMultiJackpotDescription(String gameID){
        return gson.toJson(new MultiJackpotDescription(gameID));
    }

    public static String getPayDescription(String gameID,  long totalbet, long totalPrizes, short result){
        return gson.toJson(new PayDescription(gameID,totalbet,totalPrizes,result));
    }
}
