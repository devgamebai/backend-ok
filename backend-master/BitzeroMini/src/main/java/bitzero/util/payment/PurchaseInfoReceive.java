package bitzero.util.payment;

import org.json.JSONObject;

public class PurchaseInfoReceive extends PacketPaymentReceive {
     public String PurchaseID;
     public String AccountName;
     public long ItemID;
     public int ItemQuantity;
     public String ItemName;
     public int CashAmt;
     public String PurchaseCode;
     public int Reserved;
     public long CashRemain;
     public int RetCode;

     public PurchaseInfoReceive(JSONObject data) {
          super(data);
     }
}
