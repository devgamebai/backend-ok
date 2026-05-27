package bitzero.engine.util;

import bitzero.engine.core.BitZeroEngine;

public class EngineStats {
     public static long getIncomingBytes() {
          return BitZeroEngine.getInstance().getEngineReader().getReadBytes();
     }

     public static long getIncomingPackets() {
          return BitZeroEngine.getInstance().getEngineReader().getReadPackets();
     }

     public static long getOutgoingBytes() {
          return BitZeroEngine.getInstance().getEngineWriter().getWrittenBytes();
     }

     public static long getOutgoingPackets() {
          return BitZeroEngine.getInstance().getEngineWriter().getWrittenPackets();
     }

     public static long getOutgoingDroppedPackets() {
          return BitZeroEngine.getInstance().getEngineWriter().getDroppedPacketsCount();
     }

     public static int getRestartCount() {
          return BitZeroEngine.getInstance().getRestartCount();
     }
}
