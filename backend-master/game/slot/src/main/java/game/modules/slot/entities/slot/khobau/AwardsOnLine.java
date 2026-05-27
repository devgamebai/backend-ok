package game.modules.slot.entities.slot.khobau;

public class AwardsOnLine {
     private KhoBauAward award;
     private long money;
     private String lineName;

     public AwardsOnLine(KhoBauAward award, long money, String lineName) {
          this.award = award;
          this.money = money;
          this.lineName = lineName;
     }

     public KhoBauAward getAward() {
          return this.award;
     }

     public void setAward(KhoBauAward award) {
          this.award = award;
     }

     public long getMoney() {
          return this.money;
     }

     public void setMoney(long money) {
          this.money = money;
     }

     public String getLineName() {
          return this.lineName;
     }

     public void setLineName(String lineName) {
          this.lineName = lineName;
     }

     public String getLineId() {
          return this.lineName.substring(4, this.lineName.length());
     }
}
