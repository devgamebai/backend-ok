package game.coup.server.rule;

public class Piece implements Cloneable {
   public String key = "";
   public char color;
   public char character;
   public char index;
   public int[] position = new int[2];
   public String transKey = "";
   public boolean isTrans = false;
   public boolean deadBeforeTrans = false;

   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("|key:").append(this.key);
      sb.append("|transKey:").append(this.transKey);
      sb.append("|isTrans:").append(this.isTrans);
      sb.append("|deadBeforeTrans:").append(this.deadBeforeTrans);
      return sb.toString();
   }

   public void transform() {
      if (!this.isTrans) {
         String tmp = this.key;
         this.key = this.transKey;
         this.color = this.key.charAt(0);
         this.character = this.key.charAt(1);
         this.index = this.key.charAt(2);
         this.isTrans = true;
         this.transKey = tmp;
      }

   }

   public void transback() {
      if (this.isTrans) {
         if (this.transKey.equals("rz2") || this.key.equals("rz2")) {
            boolean var1 = false;
         }

         String tmp = this.key;
         this.key = this.transKey;
         this.color = this.key.charAt(0);
         this.character = this.key.charAt(1);
         this.index = this.key.charAt(2);
         this.isTrans = false;
         this.transKey = tmp;
      }

   }

   public Piece(String name, int[] position) {
      this.key = name;
      this.color = name.charAt(0);
      this.character = name.charAt(1);
      this.index = name.charAt(2);
      this.position = position;
   }

   public Piece(Piece piece) {
      this.color = piece.color;
      this.character = piece.character;
      if (piece.index == '0') {
         if (this.character == 'z') {
            this.index = '5';
         } else {
            this.index = '2';
         }
      }

      if (piece.index == '1') {
         if (this.character == 'z') {
            this.index = '6';
         } else {
            this.index = '3';
         }
      }

      if (piece.index == '2') {
         this.index = '7';
      }

      if (piece.index == '3') {
         this.index = '8';
      }

      if (piece.index == '4') {
         this.index = '9';
      }

      StringBuilder sb = new StringBuilder();
      sb.append(this.color).append(this.character).append(this.index);
      this.key = sb.toString();
      this.position[0] = piece.position[0];
      this.position[1] = piece.position[1];
      this.isTrans = piece.isTrans;
   }

   public int hashCode() {
      return this.key.hashCode();
   }

   public boolean equals(Object o) {
      if (o instanceof Piece) {
         Piece p = (Piece)o;
         return p.key.equalsIgnoreCase(this.key);
      } else {
         return false;
      }
   }
}
