package bitzero.util.logcontroller.business;

public interface ILogController {
     void writeLog(LogMode var1, String var2);

     void writeLog(String var1, String var2);

     void writeLog(LogMode var1, String var2, String var3);

     public static enum LogMode {
          ERROR("error"),
          ACTION("action"),
          PAYMENT("payment"),
          INFO("info");

          private final String code;

          private LogMode(String code) {
               this.code = code;
          }

          public String value() {
               return this.code;
          }
     }
}
