package game.coup.server.rule;

import java.util.ArrayList;

public class Rules {
   private int[] pos;
   private Board board;
   private char player;

   public ArrayList getNextMove(String piece, int[] pos, Board board) {
      this.pos = pos;
      this.board = board;
      this.player = piece.charAt(0);
      switch(piece.charAt(1)) {
      case 'g':
         return this.generalRules();
      case 'h':
      case 'i':
      case 'j':
      case 'k':
      case 'l':
      case 'n':
      case 'o':
      case 'q':
      case 'r':
      case 'u':
      case 'v':
      case 'w':
      case 'y':
      default:
         return null;
      case 'm':
         return this.maRules();
      case 'p':
         return this.phaoRules();
      case 's':
         char indexS = piece.charAt(2);
         if (indexS != '0' && indexS != '1') {
            return this.syRules();
         }

         return this.syUpRules();
      case 't':
         char indexT = piece.charAt(2);
         if (indexT != '0' && indexT != '1') {
            return this.voiRules();
         }

         return this.voiUpRules();
      case 'x':
         return this.xeRules();
      case 'z':
         return this.totRules();
      }
   }

   private ArrayList maRules() {
      ArrayList moves = new ArrayList();
      int[][] target = new int[][]{{1, -2}, {2, -1}, {2, 1}, {1, 2}, {-1, 2}, {-2, 1}, {-2, -1}, {-1, -2}};
      int[][] obstacle = new int[][]{{0, -1}, {1, 0}, {1, 0}, {0, 1}, {0, 1}, {-1, 0}, {-1, 0}, {0, -1}};

      for(int i = 0; i < target.length; ++i) {
         int[] e = new int[]{this.pos[0] + target[i][0], this.pos[1] + target[i][1]};
         int[] f = new int[]{this.pos[0] + obstacle[i][0], this.pos[1] + obstacle[i][1]};
         if (this.board.isInside(e) && this.board.isEmpty(f)) {
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         }
      }

      return moves;
   }

   private ArrayList xeRules() {
      ArrayList moves = new ArrayList();
      int[] yOffsets = new int[]{1, 2, 3, 4, 5, 6, 7, 8};
      int[] xOffsets = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
      int[] var4 = yOffsets;
      int var5 = yOffsets.length;

      int var6;
      int offset;
      int[] dMove;
      for(var6 = 0; var6 < var5; ++var6) {
         offset = var4[var6];
         dMove = new int[]{this.pos[0], this.pos[1] + offset};
         if (!this.board.isEmpty(dMove)) {
            if (this.board.isInside(dMove) && this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }

         moves.add(dMove);
      }

      var4 = yOffsets;
      var5 = yOffsets.length;

      for(var6 = 0; var6 < var5; ++var6) {
         offset = var4[var6];
         dMove = new int[]{this.pos[0], this.pos[1] - offset};
         if (!this.board.isEmpty(dMove)) {
            if (this.board.isInside(dMove) && this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }

         moves.add(dMove);
      }

      var4 = xOffsets;
      var5 = xOffsets.length;

      for(var6 = 0; var6 < var5; ++var6) {
         offset = var4[var6];
         dMove = new int[]{this.pos[0] - offset, this.pos[1]};
         if (!this.board.isEmpty(dMove)) {
            if (this.board.isInside(dMove) && this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }

         moves.add(dMove);
      }

      var4 = xOffsets;
      var5 = xOffsets.length;

      for(var6 = 0; var6 < var5; ++var6) {
         offset = var4[var6];
         dMove = new int[]{this.pos[0] + offset, this.pos[1]};
         if (!this.board.isEmpty(dMove)) {
            if (this.board.isInside(dMove) && this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }

         moves.add(dMove);
      }

      return moves;
   }

   private ArrayList phaoRules() {
      ArrayList moves = new ArrayList();
      int[] yOffsets = new int[]{1, 2, 3, 4, 5, 6, 7, 8};
      int[] xOffsets = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
      boolean rr = false;
      boolean ll = false;
      boolean uu = false;
      boolean dd = false;
      int[] var8 = yOffsets;
      int var9 = yOffsets.length;

      int var10;
      int offset;
      int[] dMove;
      boolean e;
      for(var10 = 0; var10 < var9; ++var10) {
         offset = var8[var10];
         dMove = new int[]{this.pos[0], this.pos[1] + offset};
         if (!this.board.isInside(dMove)) {
            break;
         }

         e = this.board.isEmpty(dMove);
         if (!rr) {
            if (e) {
               moves.add(dMove);
            } else {
               rr = true;
            }
         } else if (!e) {
            if (this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }
      }

      var8 = yOffsets;
      var9 = yOffsets.length;

      for(var10 = 0; var10 < var9; ++var10) {
         offset = var8[var10];
         dMove = new int[]{this.pos[0], this.pos[1] - offset};
         if (!this.board.isInside(dMove)) {
            break;
         }

         e = this.board.isEmpty(dMove);
         if (!ll) {
            if (e) {
               moves.add(dMove);
            } else {
               ll = true;
            }
         } else if (!e) {
            if (this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }
      }

      var8 = xOffsets;
      var9 = xOffsets.length;

      for(var10 = 0; var10 < var9; ++var10) {
         offset = var8[var10];
         dMove = new int[]{this.pos[0] - offset, this.pos[1]};
         if (!this.board.isInside(dMove)) {
            break;
         }

         e = this.board.isEmpty(dMove);
         if (!uu) {
            if (e) {
               moves.add(dMove);
            } else {
               uu = true;
            }
         } else if (!e) {
            if (this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }
      }

      var8 = xOffsets;
      var9 = xOffsets.length;

      for(var10 = 0; var10 < var9; ++var10) {
         offset = var8[var10];
         dMove = new int[]{this.pos[0] + offset, this.pos[1]};
         if (!this.board.isInside(dMove)) {
            break;
         }

         e = this.board.isEmpty(dMove);
         if (!dd) {
            if (e) {
               moves.add(dMove);
            } else {
               dd = true;
            }
         } else if (!e) {
            if (this.board.getPiece(dMove).color != this.player) {
               moves.add(dMove);
            }
            break;
         }
      }

      return moves;
   }

   private ArrayList voiRules() {
      ArrayList moves = new ArrayList();
      int[][] target = new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
      int[][] obstacle = new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

      for(int i = 0; i < target.length; ++i) {
         int[] e = new int[]{this.pos[0] + target[i][0], this.pos[1] + target[i][1]};
         int[] f = new int[]{this.pos[0] + obstacle[i][0], this.pos[1] + obstacle[i][1]};
         if (this.board.isInside(e) && this.board.isEmpty(f)) {
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         }
      }

      return moves;
   }

   private ArrayList syRules() {
      ArrayList moves = new ArrayList();
      int[][] target = new int[][]{{-1, -1}, {1, 1}, {-1, 1}, {1, -1}};
      int[][] var3 = target;
      int var4 = target.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         int[] aTarget = var3[var5];
         int[] e = new int[]{this.pos[0] + aTarget[0], this.pos[1] + aTarget[1]};
         if (this.board.isInside(e)) {
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         }
      }

      return moves;
   }

   private ArrayList voiUpRules() {
      ArrayList moves = new ArrayList();
      int[][] target = new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
      int[][] obstacle = new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

      for(int i = 0; i < target.length; ++i) {
         int[] e = new int[]{this.pos[0] + target[i][0], this.pos[1] + target[i][1]};
         int[] f = new int[]{this.pos[0] + obstacle[i][0], this.pos[1] + obstacle[i][1]};
         if (this.board.isInside(e) && (e[0] <= 4 || this.player != 'b') && (e[0] >= 5 || this.player != 'r') && this.board.isEmpty(f)) {
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         }
      }

      return moves;
   }

   private ArrayList syUpRules() {
      ArrayList moves = new ArrayList();
      int[][] target = new int[][]{{-1, -1}, {1, 1}, {-1, 1}, {1, -1}};
      int[][] var3 = target;
      int var4 = target.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         int[] aTarget = var3[var5];
         int[] e = new int[]{this.pos[0] + aTarget[0], this.pos[1] + aTarget[1]};
         if (this.board.isInside(e) && (e[0] <= 2 && e[1] >= 3 && e[1] <= 5 || this.player != 'b') && (e[0] >= 7 && e[1] >= 3 && e[1] <= 5 || this.player != 'r')) {
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         }
      }

      return moves;
   }

   private ArrayList generalRules() {
      ArrayList moves = new ArrayList();
      int[][] target = new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
      int[][] var3 = target;
      int var4 = target.length;

      int i;
      for(i = 0; i < var4; ++i) {
         int[] aTarget = var3[i];
         int[] e = new int[]{this.pos[0] + aTarget[0], this.pos[1] + aTarget[1]};
         if (this.board.isInside(e) && (e[0] <= 2 && e[1] >= 3 && e[1] <= 5 || this.player != 'b') && (e[0] >= 7 && e[1] >= 3 && e[1] <= 5 || this.player != 'r')) {
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         }
      }

      boolean flag = true;
      int[] oppoBoss = this.player == 'r' ? ((Piece)this.board.pieces.get("bg0")).position : ((Piece)this.board.pieces.get("rg0")).position;
      if (oppoBoss[1] == this.pos[1]) {
         for(i = Math.min(oppoBoss[0], this.pos[0]) + 1; i < Math.max(oppoBoss[0], this.pos[0]); ++i) {
            if (this.board.getPiece(i, this.pos[1]) != null) {
               flag = false;
               break;
            }
         }

         if (flag) {
            moves.add(oppoBoss);
         }
      }

      return moves;
   }

   private ArrayList totRules() {
      ArrayList moves = new ArrayList();
      int[][] targetU = new int[][]{{0, 1}, {0, -1}, {-1, 0}};
      int[][] targetD = new int[][]{{0, 1}, {0, -1}, {1, 0}};
      int var5;
      int var6;
      int[] aTarget;
      int[] e;
      int[][] var9;
      if (this.player == 'r') {
         if (this.pos[0] > 4) {
            e = new int[]{this.pos[0] - 1, this.pos[1]};
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         } else {
            var9 = targetU;
            var5 = targetU.length;

            for(var6 = 0; var6 < var5; ++var6) {
               aTarget = var9[var6];
               e = new int[]{this.pos[0] + aTarget[0], this.pos[1] + aTarget[1]};
               if (this.board.isInside(e)) {
                  if (this.board.isEmpty(e)) {
                     moves.add(e);
                  } else if (this.board.getPiece(e).color != this.player) {
                     moves.add(e);
                  }
               }
            }
         }
      }

      if (this.player == 'b') {
         if (this.pos[0] < 5) {
            e = new int[]{this.pos[0] + 1, this.pos[1]};
            if (this.board.isEmpty(e)) {
               moves.add(e);
            } else if (this.board.getPiece(e).color != this.player) {
               moves.add(e);
            }
         } else {
            var9 = targetD;
            var5 = targetD.length;

            for(var6 = 0; var6 < var5; ++var6) {
               aTarget = var9[var6];
               e = new int[]{this.pos[0] + aTarget[0], this.pos[1] + aTarget[1]};
               if (this.board.isInside(e)) {
                  if (this.board.isEmpty(e)) {
                     moves.add(e);
                  } else if (this.board.getPiece(e).color != this.player) {
                     moves.add(e);
                  }
               }
            }
         }
      }

      return moves;
   }
}
