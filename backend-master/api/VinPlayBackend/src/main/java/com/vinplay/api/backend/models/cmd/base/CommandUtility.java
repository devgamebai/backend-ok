package com.vinplay.api.backend.models.cmd.base;

public class CommandUtility {
     public static void send(String commandName, String[] params, String host, int port) {
          CrossConnection conn = null;

          try {
               conn = new CrossConnection(host, port);
               CrossExtCommand cmd = new CrossExtCommand(commandName, params);
               conn.send(cmd.getByte());
          } catch (Exception var9) {
               var9.printStackTrace();
          } finally {
               if (conn != null) {
                    conn.close();
               }

          }

     }
}
