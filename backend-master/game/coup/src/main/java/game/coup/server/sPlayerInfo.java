package game.coup.server;

public class sPlayerInfo {
   public char chessColor = 'x';
   public int turnTime = 0;
   public int gameTime = 0;

   public void start() {
      this.turnTime = 60;
      this.gameTime = 600;
   }
}
