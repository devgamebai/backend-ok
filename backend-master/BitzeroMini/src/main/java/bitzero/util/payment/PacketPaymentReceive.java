package bitzero.util.payment;

import java.lang.reflect.Field;
import org.json.JSONException;
import org.json.JSONObject;

public class PacketPaymentReceive {
     public PacketPaymentReceive(JSONObject data) {
          this.parseJson(data);
     }

     public void parseJson(JSONObject data) {
          Field[] fields = this.getClass().getFields();
          Field[] var3 = fields;
          int var4 = fields.length;

          for(int var5 = 0; var5 < var4; ++var5) {
               Field f = var3[var5];

               try {
                    if (f.getModifiers() == 1) {
                         f.set(this, data.get(f.getName()));
                    }
               } catch (IllegalAccessException var8) {
               } catch (JSONException var9) {
               }
          }

     }
}
