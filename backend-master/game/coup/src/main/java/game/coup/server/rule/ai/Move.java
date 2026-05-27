package game.coup.server.rule.ai;

import game.coup.server.rule.GameResult;
import game.coup.server.rule.Piece;

public class Move {
   public static final int END_GAME = 1;
   public static final int CONTINUE = 2;
   public static final int ERROR_UNEXIST = 3;
   public static final int ERROR_INVALID = 4;
   public static final int ERROR_STATE = 4;
   public static final int CHIEU_CU_NHAY = 5;
   public static final int THE_HOA = 6;
   public int currentChair;
   public Piece piece;
   public int[] from;
   public int[] to;
   public Piece eatenPiece;
   public GameResult result = new GameResult();
   public boolean isTrans = false;

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.piece).append(" move from {");
      sb.append(this.from[0]).append(",").append(this.from[1]).append("}");
      sb.append(" to {");
      sb.append(this.to[0]).append(",").append(this.to[1]).append("}");
      if (this.eatenPiece != null) {
         sb.append("eatenTransPiece ").append(this.eatenPiece);
      }

      sb.append(" result: ").append(this.result.result);
      sb.append(" isTrans: ").append(this.isTrans);
      return sb.toString();
   }

   public boolean sameWith(Move m) {
      return m.piece.key == this.piece.key && (m.from[0] == this.from[0] && m.from[1] == this.from[1] && m.to[0] == this.to[0] && m.to[1] == this.to[1] || m.from[0] == this.to[0] && m.from[1] == this.to[1] && m.to[0] == this.from[0] && m.to[1] == this.from[1]);
   }

   public void transform() {
      if (!this.piece.isTrans) {
         this.isTrans = true;
         this.piece.transform();
      }

   }

   public boolean isOK() {
      return this.result.result == GameResult.Name.CONTINUE || this.result.result == GameResult.Name.DRAW || this.result.result == GameResult.Name.WIN_LOST;
   }
}
