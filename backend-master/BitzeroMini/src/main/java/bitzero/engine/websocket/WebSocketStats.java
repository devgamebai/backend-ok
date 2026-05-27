package bitzero.engine.websocket;

public class WebSocketStats {
     private volatile long readBytes = 0L;
     private volatile long readPackets = 0L;
     private volatile long writtenBytes = 0L;
     private volatile long writtenPackets = 0L;
     private volatile long droppedInPackets = 0L;
     private volatile long droppedInFrames = 0L;

     public void addDroppedInPacket() {
          ++this.droppedInPackets;
     }

     public long getDroppedInPackets() {
          return this.droppedInPackets;
     }

     public void addDroppedInFrame() {
          ++this.droppedInFrames;
     }

     public long getDroppedInFrames() {
          return this.droppedInFrames;
     }

     public void addReadBytes(int value) {
          this.readBytes += (long)value;
     }

     public void addReadPackets(int value) {
          this.readPackets += (long)value;
     }

     public void addWrittenBytes(int value) {
          this.writtenBytes += (long)value;
     }

     public void addWrittenPackets(int value) {
          this.writtenPackets += (long)value;
     }

     public long getReadBytes() {
          return this.readBytes;
     }

     public long getReadPackets() {
          return this.readPackets;
     }

     public long getWrittenBytes() {
          return this.writtenBytes;
     }

     public long getWrittenPackets() {
          return this.writtenPackets;
     }
}
