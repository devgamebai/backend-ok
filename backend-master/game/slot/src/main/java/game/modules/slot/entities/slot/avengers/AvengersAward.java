package game.modules.slot.entities.slot.avengers;

public enum AvengersAward {
     PENTA_SCATTER("PENTA_SCATTER", 0, (byte)1, AvengersItem.SCATTER, (byte)5, -3.0F),
     QUADAR_SCATTER("QUADAR_SCATTER", 1, (byte)2, AvengersItem.SCATTER, (byte)4, -3.0F),
     TRIPLE_SCATTER("TRIPLE_SCATTER", 2, (byte)3, AvengersItem.SCATTER, (byte)3, -3.0F),
     PENTA_BONUS("PENTA_BONUS", 3, (byte)4, AvengersItem.BONUS, (byte)5, -2.0F),
     QUADAR_BONUS("QUADAR_BONUS", 4, (byte)5, AvengersItem.BONUS, (byte)4, -2.0F),
     TRIPLE_BONUS("TRIPLE_BONUS", 5, (byte)6, AvengersItem.BONUS, (byte)3, -2.0F),
     PENTA_WILD("PENTA_WILD", 6, (byte)7, AvengersItem.WILD, (byte)5, 1000.0F),
     QUADAR_WILD("QUADAR_WILD", 7, (byte)8, AvengersItem.WILD, (byte)4, 80.0F),
     TRIPLE_WILD("TRIPLE_WILD", 8, (byte)9, AvengersItem.WILD, (byte)3, 25.0F),
     PENTA_JACK_POT("PENTA_JACK_POT", 9, (byte)10, AvengersItem.JACK_POT, (byte)5, -1.0F),
     QUADAR_JACK_POT("QUADAR_JACK_POT", 10, (byte)11, AvengersItem.JACK_POT, (byte)4, 100.0F),
     TRIPLE_JACK_POT("TRIPLE_JACK_POT", 11, (byte)12, AvengersItem.JACK_POT, (byte)3, 30.0F),
     PENTA_NAM_TAY("PENTA_NAM_TAY", 12, (byte)13, AvengersItem.NAM_TAY, (byte)5, 300.0F),
     QUADAR_NAM_TAY("QUADAR_NAM_TAY", 13, (byte)14, AvengersItem.NAM_TAY, (byte)4, 50.0F),
     TRIPLE_NAM_TAY("TRIPLE_NAM_TAY", 14, (byte)15, AvengersItem.NAM_TAY, (byte)3, 20.0F),
     PENTA_BUA("PENTA_BUA", 15, (byte)16, AvengersItem.BUA, (byte)5, 200.0F),
     QUADAR_BUA("QUADAR_BUA", 16, (byte)17, AvengersItem.BUA, (byte)4, 30.0F),
     TRIPLE_BUA("TRIPLE_BUA", 17, (byte)18, AvengersItem.BUA, (byte)3, 10.0F),
     PENTA_KHIEN("PENTA_KHIEN", 18, (byte)19, AvengersItem.KHIEN, (byte)5, 110.0F),
     QUADAR_KHIEN("QUADAR_KHIEN", 19, (byte)20, AvengersItem.KHIEN, (byte)4, 20.0F),
     TRIPLE_KHIEN("TRIPLE_KHIEN", 20, (byte)21, AvengersItem.KHIEN, (byte)3, 10.0F),
     PENTA_KIM_CUONg("PENTA_KIM_CUONg", 21, (byte)22, AvengersItem.KIM_CUONG, (byte)5, 50.0F),
     QUADAR_KIM_CUONg("QUADAR_KIM_CUONg", 22, (byte)23, AvengersItem.KIM_CUONG, (byte)4, 10.0F),
     TRIPLE_KIM_CUONg("TRIPLE_KIM_CUONg", 23, (byte)24, AvengersItem.KIM_CUONG, (byte)3, 6.0F),
     PENTA_DAI_BANG("PENTA_DAI_BANG", 24, (byte)25, AvengersItem.DAI_BANG, (byte)5, 30.0F),
     QUADAR_DAI_BANG("QUADAR_DAI_BANG", 25, (byte)26, AvengersItem.DAI_BANG, (byte)4, 7.0F),
     TRIPLE_DAI_BANG("TRIPLE_DAI_BANG", 26, (byte)27, AvengersItem.DAI_BANG, (byte)3, 5.0F),
     PENTA_NGUOI_NHEN("PENTA_NGUOI_NHEN", 27, (byte)28, AvengersItem.NGUOI_NHEN, (byte)5, 20.0F),
     QUADAR_NGUOI_NHEN("QUADAR_NGUOI_NHEN", 28, (byte)29, AvengersItem.NGUOI_NHEN, (byte)4, 6.0F),
     TRIPLE_NGUOI_NHEN("TRIPLE_NGUOI_NHEN", 29, (byte)30, AvengersItem.NGUOI_NHEN, (byte)3, 4.0F),
     PENTA_RADAR("PENTA_RADAR", 30, (byte)31, AvengersItem.RADAR, (byte)5, 11.0F),
     QUADAR_RADAR("QUADAR_RADAR", 31, (byte)32, AvengersItem.RADAR, (byte)4, 5.0F),
     TRIPLE_RADAR("TRIPLE_RADAR", 32, (byte)33, AvengersItem.RADAR, (byte)3, 3.0F);

     private byte id;
     private AvengersItem item;
     private byte duplicate;
     private float ratio;

     private AvengersAward(String s, int n, byte id, AvengersItem item, byte duplicate, float ratio) {
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

     public void setItem(AvengersItem item) {
          this.item = item;
     }

     public AvengersItem getItem() {
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
