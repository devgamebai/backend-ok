package game.coup.server.logic;

import game.coup.server.rule.Piece;
import game.coup.server.rule.ai.Move;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

public class ReplayGame {
   public int startChair = 0;
   public List moves = this.parseMoves();
   public int index = 0;

   public static void main(String[] args) {
      new ReplayGame();
   }

   public ReplayGame() {
      if (this.moves.size() > 0) {
         Move firstMove = (Move)this.moves.get(0);
         this.startChair = firstMove.currentChair;
      }

   }

   public Move getMove() {
      if (this.index == this.moves.size()) {
         this.index = 0;
      }

      return (Move)this.moves.get(this.index++);
   }

   public List parseMoves() {
      List moves = new LinkedList();
      String log = this.loadGameLog();
      String[] lines = log.split(">");

      for(int i = 0; i < lines.length; ++i) {
         String line = lines[i];
         String[] lineLog = line.split("<");
         if (lineLog.length == 2) {
            String action = lineLog[0];
            String detail = lineLog[1];
            if (action.contains("DQ")) {
               Move move = this.parseMove(detail);
               moves.add(move);
            }
         }
      }

      return moves;
   }

   public Move parseMove(String detailLog) {
      Move move = new Move();

      try {
         String[] infos = detailLog.split(";");
         move.currentChair = Integer.parseInt(infos[0]);
         String[] pp = infos[1].split("/");
         move.from = new int[2];
         move.from[0] = Integer.parseInt(infos[2]);
         move.from[1] = Integer.parseInt(infos[3]);
         Piece p = new Piece(pp[0], move.from);
         p.transKey = pp[1];
         move.piece = p;
         move.to = new int[2];
         move.to[0] = Integer.parseInt(infos[4]);
         move.to[1] = Integer.parseInt(infos[5]);
         if (infos.length == 8) {
            String[] ep = infos[6].split("/");
            Piece e = new Piece(ep[0], move.from);
            e.transKey = ep[1];
            move.eatenPiece = e;
         }
      } catch (Exception var8) {
         System.err.println(detailLog);
      }

      return move;
   }

   public String loadGameLog() {
      String path = System.getProperty("user.dir");
      File file = new File(path + "/conf/gameLog.txt");
      StringBuffer contents = new StringBuffer();
      BufferedReader reader = null;

      try {
         Reader r = new InputStreamReader(new FileInputStream(file), "UTF-8");
         reader = new BufferedReader(r);
         String text = null;

         while((text = reader.readLine()) != null) {
            contents.append(text).append(System.getProperty("line.separator"));
         }

         return contents.toString();
      } catch (Exception var7) {
         var7.printStackTrace();
         return "";
      }
   }
}
