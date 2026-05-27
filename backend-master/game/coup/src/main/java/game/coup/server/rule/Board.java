package game.coup.server.rule;

import game.coup.server.rule.ai.Move;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Board {
   public static final int BOARD_WIDTH = 9;
   public static final int BOARD_HEIGHT = 10;
   public Map pieces = new HashMap();
   private List redRandomPieces = new LinkedList();
   private int redIndex = -1;
   private List blackRandomPieces = new LinkedList();
   private int blackIndex = -1;
   public Map deadPieces = new HashMap();
   public List moveList = new LinkedList();
   public char player = 'b';
   public Piece[][] cells = new Piece[10][9];
   public int stateCount = 0;
   public int blackMate = 0;
   public int redMate = 0;

   public void initRandomPiece() {
      Collection allPieces = this.pieces.values();
      Iterator var2 = allPieces.iterator();

      while(var2.hasNext()) {
         Piece piece = (Piece)var2.next();
         Piece p = new Piece(piece);
         if (p.color == 'r' && p.character != 'g') {
            this.redRandomPieces.add(p);
         }

         if (p.color == 'b' && p.character != 'g') {
            this.blackRandomPieces.add(p);
         }
      }

      Collections.shuffle(this.redRandomPieces);
      Collections.shuffle(this.blackRandomPieces);
   }

   public Piece getRandomPieces(char color) {
      Piece p = null;
      if (color == 'r') {
         if (this.redIndex >= this.redRandomPieces.size()) {
            this.redIndex = this.redRandomPieces.size() - 1;
         }

         p = (Piece)this.redRandomPieces.get(++this.redIndex);
      }

      if (color == 'b') {
         if (this.blackIndex >= this.blackRandomPieces.size()) {
            this.blackIndex = this.blackRandomPieces.size() - 1;
         }

         p = (Piece)this.blackRandomPieces.get(++this.blackIndex);
      }

      return p;
   }

   public void reset() {
      this.redRandomPieces.clear();
      this.blackRandomPieces.clear();
      this.redIndex = -1;
      this.blackIndex = -1;
      this.pieces.clear();
      this.deadPieces.clear();
      this.moveList.clear();
      this.player = 'b';

      for(int i = 0; i < 10; ++i) {
         for(int j = 0; j < 9; ++j) {
            this.cells[i][j] = null;
         }
      }

      this.stateCount = 0;
   }

   public boolean isInside(int[] position) {
      return this.isInside(position[0], position[1]);
   }

   public boolean isInside(int x, int y) {
      return x >= 0 && x < 10 && y >= 0 && y < 9;
   }

   public boolean isEmpty(int[] position) {
      return this.isEmpty(position[0], position[1]);
   }

   public boolean isEmpty(int x, int y) {
      return this.isInside(x, y) && this.cells[x][y] == null;
   }

   public boolean update(Piece piece) {
      int[] pos = piece.position;
      this.cells[pos[0]][pos[1]] = piece;
      return true;
   }

   public Piece updatePiece(int[] oldPos, int[] newPos) {
      Piece orig = this.getPiece(oldPos);
      Piece inNewPos = this.getPiece(newPos);
      if (inNewPos != null) {
         if (inNewPos.isTrans) {
            this.pieces.remove(inNewPos.transKey);
            this.deadPieces.put(inNewPos.transKey, inNewPos);
         } else {
            this.pieces.remove(inNewPos.key);
            inNewPos.deadBeforeTrans = true;
            this.deadPieces.put(inNewPos.key, inNewPos);
            inNewPos.transform();
         }
      }

      int[] origPos = orig.position;
      this.cells[origPos[0]][origPos[1]] = null;
      this.cells[newPos[0]][newPos[1]] = orig;
      orig.position = newPos;
      this.player = (char)(this.player == 'r' ? 98 : 114);
      return inNewPos;
   }

   public boolean backPiece(String key) {
      int[] origPos = ((Piece)this.pieces.get(key)).position;
      this.cells[origPos[0]][origPos[1]] = (Piece)this.pieces.get(key);
      return true;
   }

   public Piece getPiece(int[] pos) {
      return this.getPiece(pos[0], pos[1]);
   }

   public Piece getPiece(int x, int y) {
      return this.cells[x][y];
   }

   public Board clone() {
      Board b = new Board();
      b.player = this.player;
      b.pieces = new HashMap();

      Iterator var2;
      Entry entry;
      Piece piece;
      for(var2 = this.pieces.entrySet().iterator(); var2.hasNext(); b.cells[piece.position[0]][piece.position[1]] = piece) {
         entry = (Entry)var2.next();
         piece = (Piece)entry.getValue();
         b.pieces.put(piece.key, piece);
      }

      var2 = this.deadPieces.entrySet().iterator();

      while(var2.hasNext()) {
         entry = (Entry)var2.next();
         piece = (Piece)entry.getValue();
         b.deadPieces.put(piece.key, piece);
      }

      return b;
   }

   public void revert(Move move) {
      this.cells[move.from[0]][move.from[1]] = move.piece;
      move.piece.position = move.from;
      if (move.isTrans) {
         move.piece.transback();
      }

      if (move.eatenPiece != null) {
         this.cells[move.to[0]][move.to[1]] = move.eatenPiece;
         if (move.eatenPiece.deadBeforeTrans) {
            move.eatenPiece.transback();
            move.eatenPiece.deadBeforeTrans = false;
         }

         if (move.eatenPiece.isTrans) {
            this.pieces.put(move.eatenPiece.transKey, move.eatenPiece);
            this.deadPieces.remove(move.eatenPiece.transKey);
         } else {
            this.pieces.put(move.eatenPiece.key, move.eatenPiece);
            this.deadPieces.remove(move.eatenPiece.key);
         }
      } else {
         this.cells[move.to[0]][move.to[1]] = null;
      }

      if (this.player == 'r') {
         this.player = 'b';
      } else {
         this.player = 'r';
      }

   }

   public byte[][] getMap() {
      byte[][] map = new byte[10][9];

      for(int i = 0; i < 10; ++i) {
         for(int j = 0; j < 9; ++j) {
            Piece p = this.cells[i][j];
            map[i][j] = (byte)p.character;
         }
      }

      return new byte[0][0];
   }

   public String[][] getMapKey() {
      String[][] map = new String[10][9];

      for(int i = 0; i < 10; ++i) {
         for(int j = 0; j < 9; ++j) {
            Piece p = this.cells[i][j];
            if (p != null) {
               map[i][j] = p.key;
            } else {
               map[i][j] = "";
            }
         }
      }

      return map;
   }

   public byte[][] getMapStatus() {
      byte[][] map = new byte[10][9];

      for(int i = 0; i < 10; ++i) {
         for(int j = 0; j < 9; ++j) {
            Piece p = this.cells[i][j];
            if (p != null) {
               map[i][j] = (byte)(p.isTrans ? 2 : 1);
            } else {
               map[i][j] = 0;
            }
         }
      }

      return map;
   }

   public int checkLoopMove(Move move) {
      int size = this.moveList.size();
      int rangeCheck = 12;
      if (size < rangeCheck) {
         return 0;
      } else {
         int min = size - rangeCheck;
         int c = 0;

         for(int i = size - 1; i >= min; --i) {
            Move m = (Move)this.moveList.get(i);
            if (m.sameWith(move)) {
               ++c;
            }
         }

         if (c >= 3) {
            return 5;
         } else {
            return 0;
         }
      }
   }

   public Move getLastMove() {
      int size = this.moveList.size();
      return size > 0 ? (Move)this.moveList.get(size - 1) : null;
   }

   public Move getPreviousMove(Move move) {
      int size = this.moveList.size();

      for(int i = size - 1; i >= 0; --i) {
         Move m = (Move)this.moveList.get(i);
         if (m.piece.color == move.piece.color) {
            return m;
         }
      }

      return null;
   }

   public String[] getDeadPieces(char color) {
      List halfDie = new ArrayList();
      Iterator var3 = this.deadPieces.entrySet().iterator();

      while(var3.hasNext()) {
         Entry entry = (Entry)var3.next();
         Piece p = (Piece)entry.getValue();
         if (p.color != color) {
            halfDie.add(p);
         }
      }

      String[] res = new String[halfDie.size()];

      for(int i = 0; i < halfDie.size(); ++i) {
         res[i] = ((Piece)halfDie.get(i)).key;
      }

      return res;
   }

   Piece updatePiece(int[] oldPos, int[] newPos, Move mv) {
      Piece orig = this.getPiece(oldPos);
      Piece inNewPos = this.getPiece(newPos);
      if (inNewPos != null) {
         if (inNewPos.isTrans) {
            inNewPos.transKey = mv.eatenPiece.transKey;
            inNewPos.key = mv.eatenPiece.key;
            this.pieces.remove(inNewPos.transKey);
            this.deadPieces.put(inNewPos.transKey, inNewPos);
         } else {
            inNewPos.transKey = mv.eatenPiece.key;
            inNewPos.key = mv.eatenPiece.transKey;
            this.pieces.remove(inNewPos.key);
            inNewPos.deadBeforeTrans = true;
            this.deadPieces.put(inNewPos.key, inNewPos);
            inNewPos.transform();
         }
      }

      int[] origPos = orig.position;
      this.cells[origPos[0]][origPos[1]] = null;
      this.cells[newPos[0]][newPos[1]] = orig;
      orig.position = newPos;
      this.player = (char)(this.player == 'r' ? 98 : 114);
      return inNewPos;
   }
}
