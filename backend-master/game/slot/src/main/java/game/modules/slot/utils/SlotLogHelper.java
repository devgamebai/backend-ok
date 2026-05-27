package game.modules.slot.utils;

import com.vinplay.vbee.common.mongodb.MongoDBConnectionFactory;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Direct MongoDB logging for slot games.
 * Writes to "log_{gameName}" collection in win123club database.
 * Avoids SlotMachineService classpath issues.
 */
public class SlotLogHelper {

    public static void logSpin(String gameName, long referenceId, String username, long betValue,
                               String linesBetting, String linesWin, String prizesOnLine,
                               short result, long totalPrizes, String timeLog) {
        try {
            MongoDatabase db = MongoDBConnectionFactory.getDB();
            Document doc = new Document();
            doc.append("reference_id", referenceId);
            doc.append("user_name", username);
            doc.append("bet_value", betValue);
            doc.append("lines_betting", linesBetting);
            doc.append("lines_win", linesWin);
            doc.append("prizes_on_line", prizesOnLine);
            doc.append("result", (int) result);
            doc.append("prize", totalPrizes);
            doc.append("time_log", timeLog);
            doc.append("create_time", new java.util.Date());
            db.getCollection("log_" + gameName).insertOne(doc);
        } catch (Exception e) {
            // Silently ignore log failures — don't crash the game
        }
    }
}
