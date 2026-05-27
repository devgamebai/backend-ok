package bitzero.util.payment;

import java.lang.reflect.Field;
import org.json.JSONException;
import org.json.JSONObject;

public class PacketPaymentSend {
     public JSONObject toJSONObject() {
          JSONObject data = new JSONObject();
          Field[] fields = this.getClass().getFields();
          Field[] var3 = fields;
          int var4 = fields.length;

          for(int var5 = 0; var5 < var4; ++var5) {
               Field f = var3[var5];

               try {
                    if (f.getModifiers() == 1) {
                         data.put(f.getName(), f.get(this));
                    }
               } catch (IllegalAccessException var8) {
               } catch (JSONException var9) {
               }
          }

          return data;
     }

     public String toString() {
          return this.toJSONObject().toString();
     }
}
