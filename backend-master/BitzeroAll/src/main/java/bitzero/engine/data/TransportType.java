package bitzero.engine.data;

public enum TransportType {
     TCP("Tcp"),
     UDP("Udp"),
     BLUEBOX("BlueBox");

     String name;

     private TransportType(String name) {
          this.name = name;
     }

     public static TransportType fromName(String name) {
          TransportType[] atransporttype;
          int j = (atransporttype = values()).length;

          for(int i = 0; i < j; ++i) {
               TransportType tt = atransporttype[i];
               if (tt.name.equalsIgnoreCase(name)) {
                    return tt;
               }
          }

          throw new IllegalArgumentException("There is no TransportType definition for the requested type: " + name);
     }
}
