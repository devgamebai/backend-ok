package bitzero.server.util;

import java.util.HashMap;
import java.util.PriorityQueue;

public class PacketCount {
     public HashMap packetIn = new HashMap();
     public HashMap packetOut = new HashMap();
     public HashMap packetInDropped = new HashMap();
     public HashMap packetOutDroppedFull = new HashMap();
     public HashMap packetOutDroppedWarn = new HashMap();
     public int id_max = -1;
     public long num_max = 0L;
     public int id_second = -2;
     public long num_second = 0L;

     public void addPacket(int type, Short packetId) {
          if (type == PacketType.INCOMING) {
               if (this.packetIn.containsKey(packetId)) {
                    this.packetIn.put(packetId, (Long)this.packetIn.get(packetId) + 1L);
               } else {
                    this.packetIn.put(packetId, 1L);
               }
          } else if (type == PacketType.OUTGOING) {
               if (this.packetOut.containsKey(packetId)) {
                    this.packetOut.put(packetId, (Long)this.packetOut.get(packetId) + 1L);
               } else {
                    this.packetOut.put(packetId, 1L);
               }
          } else if (type == PacketType.IN_DROP) {
               if (this.packetInDropped.containsKey(packetId)) {
                    this.packetInDropped.put(packetId, (Long)this.packetInDropped.get(packetId) + 1L);
               } else {
                    this.packetInDropped.put(packetId, 1L);
               }
          } else if (type == PacketType.OUT_DROP_FULL) {
               if (this.packetOutDroppedFull.containsKey(packetId)) {
                    this.packetOutDroppedFull.put(packetId, (Long)this.packetOutDroppedFull.get(packetId) + 1L);
               } else {
                    this.packetOutDroppedFull.put(packetId, 1L);
               }
          } else if (type == PacketType.OUT_DROP_WARN) {
               if (this.packetOutDroppedWarn.containsKey(packetId)) {
                    this.packetOutDroppedWarn.put(packetId, (Long)this.packetOutDroppedWarn.get(packetId) + 1L);
               } else {
                    this.packetOutDroppedWarn.put(packetId, 1L);
               }
          }

     }

     public void addPacket(int type, Short packetId, int userId) {
          if (type == PacketType.OUT_DROP_WARN) {
               long cur = 1L;
               if (this.packetOutDroppedWarn.containsKey(packetId)) {
                    cur = (Long)this.packetOutDroppedWarn.get(packetId) + 1L;
               }

               this.packetOutDroppedWarn.put(packetId, cur);
               if (cur > this.num_second && userId != this.id_max) {
                    this.num_second = cur;
                    this.id_second = userId;
               }

               if (this.num_second > this.num_max) {
                    int temp = this.id_max;
                    long temp_ = this.num_max;
                    this.num_max = this.num_second;
                    this.id_max = this.id_second;
                    this.num_second = temp_;
                    this.id_second = temp;
               }
          }

     }

     public String countUserDrop() {
          new PriorityQueue();
          return "";
     }
}
