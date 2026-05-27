package bitzero.server.util;

public enum ClientDisconnectionReason implements IDisconnectionReason {
     IDLE(0),
     KICK(1),
     BAN(2),
     LOGIN(3),
     UNKNOWN(4),
     HANDSHAKE(5);

     private final int value;

     private ClientDisconnectionReason(int id) {
          this.value = id;
     }

     public int getValue() {
          return this.value;
     }

     public byte getByteValue() {
          return (byte)this.value;
     }
}
