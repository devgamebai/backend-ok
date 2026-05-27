package game.modules.slot.entities.slot.ndv;

public enum NDVItem {
     NONE("NONE", 0, "none", (byte)-1),
     NU_DIEP_VIEN("NU_DIEP_VIEN", 1, "NuDiepVien", (byte)0),
     THAY_THE("THAY_THE", 2, "ThayThe", (byte)1),
     KIEM_NHAT("KIEM_NHAT", 3, "KiemNhat", (byte)2),
     SUNG_DOI("SUNG_DOI", 4, "SungDoi", (byte)3),
     AO_GIAP("AO_GIAP", 5, "AoGiap", (byte)4),
     LUU_DAN("LUU_DAN", 6, "LuuDan", (byte)5),
     MU("MU", 7, "Mu", (byte)6),
     ONG_NHOM("ONG_NHOM", 8, "OngNhom", (byte)7),
     GIAY("GIAY", 9, "Giay", (byte)8);

     private String name;
     private byte id;

     private NDVItem(String s, int n, String name, byte id) {
          this.name = name;
          this.id = id;
     }

     public void setName(String name) {
          this.name = name;
     }

     public String getName() {
          return this.name;
     }

     public void setId(byte id) {
          this.id = id;
     }

     public byte getId() {
          return this.id;
     }

     public static NDVItem findItem(byte id) {
          NDVItem[] values;
          int length = (values = values()).length;

          for(int i = 0; i < length; ++i) {
               NDVItem entry = values[i];
               if (entry.getId() == id) {
                    return entry;
               }
          }

          return null;
     }
}
