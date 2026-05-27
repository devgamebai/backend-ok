package bitzero.util.payment;

import org.json.JSONObject;

public class PromoPacketReceive extends PacketPaymentReceive {
     public String CashID;
     public String AccountName;
     public String AccountNumb;
     public int CashAmt;
     public String CashCode;
     public int PromoCampaignId;
     public long CashRemain;
     public int RetCode;

     public PromoPacketReceive(JSONObject data) {
          super(data);
     }
}
