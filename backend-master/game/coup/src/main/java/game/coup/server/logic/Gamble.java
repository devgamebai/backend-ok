package game.coup.server.logic;

import game.coup.server.rule.Board;
import game.coup.server.rule.GameController;
import game.modules.gameRoom.entities.GameRoomIdGenerator;

public class Gamble {
   public CardSuit suit = new CardSuit();
   public GameController controller = new GameController();
   public Board board = new Board();
   public int id;
   public boolean isCheat = false;
   public long logTime;

   private static int getID() {
      int id = GameRoomIdGenerator.instance().getId();
      return id;
   }

   public Gamble() {
      this.reset();
   }

   public void reset() {
      this.id = getID();
      this.logTime = System.currentTimeMillis();
      this.board.reset();
      this.controller.initBoard(this.board);
   }
}
