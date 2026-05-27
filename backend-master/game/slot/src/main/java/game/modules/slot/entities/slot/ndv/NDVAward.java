package game.modules.slot.entities.slot.ndv;

public enum NDVAward {
     PENTA_NU_DIEP_VIEN("PENTA_NU_DIEP_VIEN", 0, (byte)1, NDVItem.NU_DIEP_VIEN, (byte)5, -1.0F),
     QUADRA_NU_DIEP_VIEN("QUADRA_NU_DIEP_VIEN", 1, (byte)2, NDVItem.NU_DIEP_VIEN, (byte)4, 100.0F),
     TRIPLE_NU_DIEP_VIEN("TRIPLE_NU_DIEP_VIEN", 2, (byte)3, NDVItem.NU_DIEP_VIEN, (byte)3, 30.0F),
     PENTA_THAY_THE("PENTA_THAY_THE", 3, (byte)4, NDVItem.THAY_THE, (byte)5, 1000.0F),
     QUADRA_THAY_THE("QUADRA_THAY_THE", 4, (byte)5, NDVItem.THAY_THE, (byte)4, 80.0F),
     TRIPLE_THAY_THE("TRIPLE_THAY_THE", 5, (byte)6, NDVItem.THAY_THE, (byte)3, 25.0F),
     PENTA_KIEM_NHAT("PENTA_KIEM_NHAT", 6, (byte)7, NDVItem.KIEM_NHAT, (byte)5, 500.0F),
     QUADRA_KIEM_NHAT("QUADRA_KIEM_NHAT", 7, (byte)8, NDVItem.KIEM_NHAT, (byte)4, -2.0F),
     TRIPLE_KIEM_NHAT("TRIPLE_KIEM_NHAT", 8, (byte)9, NDVItem.KIEM_NHAT, (byte)3, 15.0F),
     PENTA_SUNG_DOI("PENTA_SUNG_DOI", 9, (byte)10, NDVItem.SUNG_DOI, (byte)5, 200.0F),
     QUADRA_SUNG_DOI("QUADRA_SUNG_DOI", 10, (byte)11, NDVItem.SUNG_DOI, (byte)4, 15.0F),
     TRIPLE_SUNG_DOI("TRIPLE_SUNG_DOI", 11, (byte)12, NDVItem.SUNG_DOI, (byte)3, 8.0F),
     PENTA_AO_GIAP("PENTA_AO_GIAP", 12, (byte)13, NDVItem.AO_GIAP, (byte)5, 80.0F),
     QUADRA_AO_GIAP("QUADRA_AO_GIAP", 13, (byte)14, NDVItem.AO_GIAP, (byte)4, 10.0F),
     TRIPLE_AO_GIAP("TRIPLE_AO_GIAP", 14, (byte)15, NDVItem.AO_GIAP, (byte)3, 7.0F),
     PENTA_LUU_DAN("PENTA_LUU_DAN", 15, (byte)16, NDVItem.LUU_DAN, (byte)5, 30.0F),
     QUADRA_LUU_DAN("QUADRA_LUU_DAN", 16, (byte)17, NDVItem.LUU_DAN, (byte)4, 6.0F),
     TRIPLE_LUU_DAN("TRIPLE_LUU_DAN", 17, (byte)18, NDVItem.LUU_DAN, (byte)3, 3.0F),
     PENTA_MU("PENTA_MU", 18, (byte)19, NDVItem.MU, (byte)5, 15.0F),
     QUADRA_MU("QUADRA_MU", 19, (byte)20, NDVItem.MU, (byte)4, 5.0F),
     TRIPLE_MU("TRIPLE_MU", 20, (byte)21, NDVItem.MU, (byte)3, 2.0F),
     PENTA_ONG_NHOM("PENTA_ONG_NHOM", 21, (byte)22, NDVItem.ONG_NHOM, (byte)5, 12.0F),
     QUADRA_ONG_NHOM("QUADRA_ONG_NHOM", 22, (byte)23, NDVItem.ONG_NHOM, (byte)4, 4.0F),
     TRIPLE_ONG_NHOM("TRIPLE_ONG_NHOM", 23, (byte)24, NDVItem.ONG_NHOM, (byte)3, 1.0F),
     PENTA_GIAY("PENTA_GIAY", 24, (byte)25, NDVItem.GIAY, (byte)5, 8.0F),
     QUADRA_GIAY("QUADRA_GIAY", 25, (byte)26, NDVItem.GIAY, (byte)4, 3.0F),
     TRIPLE_GIAY("TRIPLE_GIAY", 26, (byte)27, NDVItem.GIAY, (byte)3, 1.0F);

     private byte id;
     private NDVItem item;
     private byte duplicate;
     private float ratio;

     private NDVAward(String s, int n, byte id, NDVItem item, byte duplicate, float ratio) {
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

     public void setItem(NDVItem item) {
          this.item = item;
     }

     public NDVItem getItem() {
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
