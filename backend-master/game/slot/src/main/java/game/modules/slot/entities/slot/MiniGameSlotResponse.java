package game.modules.slot.entities.slot;

public class MiniGameSlotResponse {
     private long totalPrize;
     private String prizes;
     private int ratio;

     public void setTotalPrize(long totalPrize) {
          this.totalPrize = totalPrize;
     }

     public long getTotalPrize() {
          return this.totalPrize;
     }

     public void setPrizes(String prizes) {
          this.prizes = prizes;
     }

     public String getPrizes() {
          return this.prizes;
     }

     public void setRatio(int ratio) {
          this.ratio = ratio;
     }

     public int getRatio() {
          return this.ratio;
     }
}
