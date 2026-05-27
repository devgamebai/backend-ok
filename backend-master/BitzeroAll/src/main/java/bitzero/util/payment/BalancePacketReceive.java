package bitzero.util.payment;

import org.json.JSONObject;

public class BalancePacketReceive extends PacketPaymentReceive {
     public String AccountName;
     public long CashRemain;
     public int RetCode;

     public BalancePacketReceive(JSONObject data) {
          super(data);
     }
}
