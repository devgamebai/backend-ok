package game.modules.slot.entities.slot.khobau;

public enum KhoBauAward {
     PENTA_BAG("PENTA_BAG", 0, (byte)1, KhoBauItem.BAG, (byte)5, 1000.0F),
     PENTA_POUCH("PENTA_POUCH", 1, (byte)1, KhoBauItem.POUCH, (byte)5, -2.0F),
     QUADRA_POUCH("QUADRA_POUCH", 2, (byte)1, KhoBauItem.POUCH, (byte)4, 40.0F),
     TRIPLE_POUCH("TRIPLE_POUCH", 3, (byte)1, KhoBauItem.POUCH, (byte)3, 5.0F),
     PENTA_BOOK("PENTA_BOOK", 4, (byte)1, KhoBauItem.BOOK, (byte)5, 1000.0F),
     QUADRA_BOOK("QUADRA_BOOK", 5, (byte)1, KhoBauItem.BOOK, (byte)4, -1.0F),
     TRIPLE_BOOK("TRIPLE_BOOK", 6, (byte)1, KhoBauItem.BOOK, (byte)3, 4.0F),
     PENTA_BULLSEYE("PENTA_BULLSEYE", 7, (byte)1, KhoBauItem.BULLSEYE, (byte)5, 200.0F),
     QUADRA_BULLSEYE("QUADRA_BULLSEYE", 8, (byte)1, KhoBauItem.BULLSEYE, (byte)4, 20.0F),
     TRIPLE_BULLSEYE("TRIPLE_BULLSEYE", 9, (byte)1, KhoBauItem.BULLSEYE, (byte)3, 3.0F),
     PENTA_MAP("PENTA_MAP", 10, (byte)1, KhoBauItem.MAP, (byte)5, 55.0F),
     QUADRA_MAP("QUADRA_MAP", 11, (byte)1, KhoBauItem.MAP, (byte)4, 8.0F),
     TRIPLE_MAP("TRIPLE_MAP", 12, (byte)1, KhoBauItem.MAP, (byte)3, 2.0F),
     PENTA_BOTTLE("PENTA_BOTTLE", 13, (byte)1, KhoBauItem.BOTTLE, (byte)5, 15.0F),
     QUADRA_BOTTLE("QUADRA_BOTTLE", 14, (byte)1, KhoBauItem.BOTTLE, (byte)4, 4.0F),
     PENTA_ANVIL("PENTA_ANVIL", 15, (byte)1, KhoBauItem.ANVIL, (byte)5, 8.0F),
     QUADRA_ANVIL("QUADRA_ANVIL", 16, (byte)1, KhoBauItem.ANVIL, (byte)4, 3.0F);

     private byte id;
     private KhoBauItem item;
     private byte duplicate;
     private float ratio;

     private KhoBauAward(String s, int n, byte id, KhoBauItem item, byte duplicate, float ratio) {
          this.id = id;
          this.item = item;
          this.duplicate = duplicate;
          this.ratio = ratio;
     }

     public void setId(byte id) {
          this.id = id;
     }

     public byte getId() {
          return this.id;
     }

     public void setItem(KhoBauItem item) {
          this.item = item;
     }

     public KhoBauItem getItem() {
          return this.item;
     }

     public void setDuplicate(byte duplicate) {
          this.duplicate = duplicate;
     }

     public byte getDuplicate() {
          return this.duplicate;
     }

     public void setRatio(float ratio) {
          this.ratio = ratio;
     }

     public float getRatio() {
          return this.ratio;
     }
}
