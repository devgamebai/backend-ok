package game.coup.server.rule;

import bitzero.util.common.business.Debug;
import game.coup.server.rule.ai.Move;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GameController {
   public Rules rule = new Rules();

   public static void main(String[] args) {
      GameController controller = new GameController();
      Board board = new Board();
      controller.initBoard(board);
      controller.printBoard(board);
   }

   private Map initPieces(Board board) {
      Map pieces = board.pieces;
      pieces.put("bx0", new Piece("bx0", new int[]{0, 0}));
      pieces.put("bm0", new Piece("bm0", new int[]{0, 1}));
      pieces.put("bt0", new Piece("bt0", new int[]{0, 2}));
      pieces.put("bs0", new Piece("bs0", new int[]{0, 3}));
      Piece bg0 = new Piece("bg0", new int[]{0, 4});
      bg0.isTrans = true;
      pieces.put("bg0", bg0);
      pieces.put("bs1", new Piece("bs1", new int[]{0, 5}));
      pieces.put("bt1", new Piece("bt1", new int[]{0, 6}));
      pieces.put("bm1", new Piece("bm1", new int[]{0, 7}));
      pieces.put("bx1", new Piece("bx1", new int[]{0, 8}));
      pieces.put("bp0", new Piece("bp0", new int[]{2, 1}));
      pieces.put("bp1", new Piece("bp1", new int[]{2, 7}));
      pieces.put("bz0", new Piece("bz0", new int[]{3, 0}));
      pieces.put("bz1", new Piece("bz1", new int[]{3, 2}));
      pieces.put("bz2", new Piece("bz2", new int[]{3, 4}));
      pieces.put("bz3", new Piece("bz3", new int[]{3, 6}));
      pieces.put("bz4", new Piece("bz4", new int[]{3, 8}));
      pieces.put("rx0", new Piece("rx0", new int[]{9, 0}));
      pieces.put("rm0", new Piece("rm0", new int[]{9, 1}));
      pieces.put("rt0", new Piece("rt0", new int[]{9, 2}));
      pieces.put("rs0", new Piece("rs0", new int[]{9, 3}));
      Piece rg0 = new Piece("rg0", new int[]{9, 4});
      rg0.isTrans = true;
      pieces.put("rg0", rg0);
      pieces.put("rs1", new Piece("rs1", new int[]{9, 5}));
      pieces.put("rt1", new Piece("rt1", new int[]{9, 6}));
      pieces.put("rm1", new Piece("rm1", new int[]{9, 7}));
      pieces.put("rx1", new Piece("rx1", new int[]{9, 8}));
      pieces.put("rp0", new Piece("rp0", new int[]{7, 1}));
      pieces.put("rp1", new Piece("rp1", new int[]{7, 7}));
      pieces.put("rz0", new Piece("rz0", new int[]{6, 0}));
      pieces.put("rz1", new Piece("rz1", new int[]{6, 2}));
      pieces.put("rz2", new Piece("rz2", new int[]{6, 4}));
      pieces.put("rz3", new Piece("rz3", new int[]{6, 6}));
      pieces.put("rz4", new Piece("rz4", new int[]{6, 8}));
      board.initRandomPiece();
      //char c = true;
      Iterator var6 = pieces.entrySet().iterator();

      while(var6.hasNext()) {
         Entry entry = (Entry)var6.next();
         Piece p = (Piece)entry.getValue();
         if (!p.isTrans) {
            Piece trans = board.getRandomPieces(p.color);
            p.transKey = trans.key;
         }
      }

      return pieces;
   }

   public void initBoard(Board board, String[][] names) {
      Map pieces = board.pieces;
      int c = 0;

      for(int i = 0; i < names.length; ++i) {
         for(int j = 0; j < names[i].length; ++j) {
            String p = names[i][j];
            if (!p.equals(".")) {
               Piece pi = new Piece(p, new int[]{i, j});
               pi.isTrans = true;
               if (pi.character == 'g') {
                  pieces.put(p, pi);
               } else {
                  pieces.put(p + c++, pi);
               }
            }
         }
      }

      Iterator var9 = board.pieces.entrySet().iterator();

      while(var9.hasNext()) {
         Entry stringPieceEntry = (Entry)var9.next();
         board.update((Piece)stringPieceEntry.getValue());
      }

   }

   public void initBoard(Board board) {
      this.initPieces(board);
      Iterator var2 = board.pieces.entrySet().iterator();

      while(var2.hasNext()) {
         Entry stringPieceEntry = (Entry)var2.next();
         board.update((Piece)stringPieceEntry.getValue());
      }

   }

   public Move moveChess(int[] from, int[] to, Board board) {
      Move move = new Move();
      Piece p = board.getPiece(from);
      if (p != null) {
         move.piece = p;
         move.from = p.position;
         ArrayList canMoves = this.rule.getNextMove(p.key, from, board);
         boolean valid = false;
         Iterator var8 = canMoves.iterator();

         while(var8.hasNext()) {
            int[] m = (int[])var8.next();
            if (m[0] == to[0] && m[1] == to[1]) {
               valid = true;
               break;
            }
         }

         if (p.color != board.player) {
            valid = false;
         }

         if (valid) {
            move.to = to;
            Debug.trace(new Object[]{"board player before: ", board.player});
            move.eatenPiece = board.updatePiece(p.position, to);
            Debug.trace(new Object[]{"board player later: ", board.player});
            int check = this.checkMove(board, move);
            boolean chieuCuNhay = false;
            if (check != 0) {
               if (check == 6) {
                  move.result.result = GameResult.Name.DRAW;
                  return move;
               }

               if (check == 5) {
                  chieuCuNhay = true;
               }
            }

            boolean state = this.checkValidState(board) && !chieuCuNhay;
            if (!state) {
               board.revert(move);
               move.result.result = GameResult.Name.ERROR_STATE;
            } else {
               move.transform();
               boolean endGame = this.checkGameEnd(board);
               if (endGame) {
                  move.result.result = GameResult.Name.WIN_LOST;
               } else {
                  move.result.result = GameResult.Name.CONTINUE;
               }

               board.moveList.add(move);
            }
         } else {
            move.to = to;
            move.result.result = GameResult.Name.ERROR_INVALID;
         }
      } else {
         move.from = from;
         move.to = to;
         move.result.result = GameResult.Name.ERROR_UNEXIST;
      }

      return move;
   }

   public boolean checkValidState(Board board) {
      String general = "rg0";
      if (board.player == 'r') {
         general = "bg0";
      }

      List halfPieces = new ArrayList();
      Iterator var4 = board.pieces.entrySet().iterator();

      Piece p;
      while(var4.hasNext()) {
         Entry entry = (Entry)var4.next();
         p = (Piece)entry.getValue();
         if (p.key.charAt(0) == board.player) {
            halfPieces.add(p);
         }
      }

      Piece enemyGeneral = (Piece)board.pieces.get(general);
      Iterator var11 = halfPieces.iterator();

      while(var11.hasNext()) {
         p = (Piece)var11.next();
         ArrayList moves = this.rule.getNextMove(p.key, p.position, board);
         Iterator var8 = moves.iterator();

         while(var8.hasNext()) {
            int[] m = (int[])var8.next();
            if (m[0] == enemyGeneral.position[0] && m[1] == enemyGeneral.position[1]) {
               return false;
            }
         }
      }

      return true;
   }

   public int checkMove(Board board, Move move) {
      if (move.eatenPiece == null) {
         ++board.stateCount;
         if (board.stateCount >= 120) {
            return 6;
         }
      } else {
         board.stateCount = 0;
      }

      char key = 98;
      String general = "rg0";
      if (board.player == 'b') {
         key = 114;
         general = "bg0";
      }

      List halfPieces = new ArrayList();
      Iterator var6 = board.pieces.entrySet().iterator();

      Piece chieuPiece;
      while(var6.hasNext()) {
         Entry entry = (Entry)var6.next();
         chieuPiece = (Piece)entry.getValue();
         if (chieuPiece.key.charAt(0) == key) {
            halfPieces.add(chieuPiece);
         }
      }

      Piece enemyGeneral = (Piece)board.pieces.get(general);
      boolean chieuTuong = false;
      chieuPiece = null;
      Iterator var9 = halfPieces.iterator();

      while(var9.hasNext()) {
         Piece p = (Piece)var9.next();
         ArrayList moves = this.rule.getNextMove(p.key, p.position, board);
         Iterator var12 = moves.iterator();

         while(var12.hasNext()) {
            int[] m = (int[])var12.next();
            if (m[0] == enemyGeneral.position[0] && m[1] == enemyGeneral.position[1]) {
               chieuTuong = true;
               chieuPiece = p;
               break;
            }
         }

         if (chieuTuong) {
            break;
         }
      }

      if (!chieuTuong) {
         if (move.piece.color == 'r') {
            board.redMate = 0;
         } else {
            board.blackMate = 0;
         }
      } else {
         Move m = board.getPreviousMove(move);
         if (move.piece.color == 'b') {
            if (m != null && !m.piece.key.equalsIgnoreCase(chieuPiece.key)) {
               board.blackMate = 0;
            } else {
               ++board.blackMate;
            }
         } else if (m != null && !m.piece.key.equalsIgnoreCase(chieuPiece.key)) {
            board.redMate = 0;
         } else {
            ++board.redMate;
         }

         if (board.blackMate > 3 || board.redMate > 3) {
            return 5;
         }
      }

      return 0;
   }

   public boolean checkGameEnd(Board board) {
      for(int i = 0; i < 10; ++i) {
         for(int j = 0; j < 9; ++j) {
            Piece p = board.cells[i][j];
            if (p != null && p.color == board.player) {
               int[] from = new int[]{i, j};
               ArrayList moves = this.rule.getNextMove(p.key, from, board);
               Iterator var7 = moves.iterator();

               while(var7.hasNext()) {
                  int[] to = (int[])var7.next();
                  Move move = new Move();
                  move.from = from;
                  move.to = to;
                  move.piece = p;
                  move.eatenPiece = board.updatePiece(p.position, to);
                  boolean state = this.checkValidState(board);
                  board.revert(move);
                  if (state) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   public void printBoard(Board board) {
      Debug.trace(new Object[]{"Player: ", board.player});
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < 10; ++i) {
         sb.append("\n ");

         for(int j = 0; j < 9; ++j) {
            Piece p = board.getPiece(i, j);
            if (p != null) {
               sb.append(p.color).append(p.character).append(p.index).append(" ");
            } else {
               sb.append(".  ").append(" ");
            }
         }
      }

      Debug.trace(new Object[]{"Board:\n", sb.toString()});
   }

   public char hasWin(Board board) {
      boolean isRedWin = board.pieces.get("bg0") == null;
      boolean isBlackWin = board.pieces.get("rg0") == null;
      if (isRedWin) {
         return 'r';
      } else {
         return (char)(isBlackWin ? 'b' : 'x');
      }
   }

   public Move moveChess(Move mv, Board board) {
      int[] from = mv.from;
      int[] to = mv.to;
      Move move = new Move();
      Piece p = board.getPiece(from);
      if (p != null) {
         if (p.isTrans) {
            p.key = mv.piece.key;
            p.transKey = mv.piece.transKey;
         } else {
            p.key = mv.piece.transKey;
            p.transKey = mv.piece.key;
         }

         move.piece = p;
         move.from = p.position;
         ArrayList canMoves = this.rule.getNextMove(p.key, from, board);
         boolean valid = false;
         Iterator var9 = canMoves.iterator();

         while(var9.hasNext()) {
            int[] m = (int[])var9.next();
            if (m[0] == to[0] && m[1] == to[1]) {
               valid = true;
               break;
            }
         }

         if (p.color != board.player) {
            valid = false;
         }

         if (valid) {
            move.to = to;
            Debug.trace(new Object[]{"board player before: ", board.player});
            move.eatenPiece = board.updatePiece(p.position, to, mv);
            Debug.trace(new Object[]{"board player later: ", board.player});
            int check = this.checkMove(board, move);
            boolean chieuCuNhay = false;
            if (check != 0) {
               if (check == 6) {
                  move.result.result = GameResult.Name.DRAW;
                  return move;
               }

               if (check == 5) {
                  chieuCuNhay = true;
                  Debug.trace(new Object[]{"Chieu cu nhay ==============XXXXX============", chieuCuNhay});
               }
            }

            boolean state = this.checkValidState(board) && !chieuCuNhay;
            if (!state) {
               board.revert(move);
               move.result.result = GameResult.Name.ERROR_STATE;
            } else {
               move.transform();
               boolean endGame = this.checkGameEnd(board);
               if (endGame) {
                  move.result.result = GameResult.Name.WIN_LOST;
               } else {
                  move.result.result = GameResult.Name.CONTINUE;
               }

               board.moveList.add(move);
            }
         } else {
            move.to = to;
            move.result.result = GameResult.Name.ERROR_INVALID;
         }
      } else {
         move.from = from;
         move.to = to;
         move.result.result = GameResult.Name.ERROR_UNEXIST;
      }

      return move;
   }
}
