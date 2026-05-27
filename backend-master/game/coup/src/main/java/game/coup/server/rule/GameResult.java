package game.coup.server.rule;

public class GameResult {
   public GameResult.Name result;

   public static enum Name {
      WIN_LOST,
      DRAW,
      CONTINUE,
      ERROR_UNEXIST,
      ERROR_INVALID,
      ERROR_STATE,
      TIME_OUT,
      RESIGN;
   }
}
