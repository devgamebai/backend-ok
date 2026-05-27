package bitzero.engine.data;

public interface IPacketBuffer {
     boolean isMultiSegment();

     boolean hasMoreSegments();

     int getRemaining();

     int getPosition();

     int getSize();

     byte[] getSegment();

     void setSegment(byte[] var1);

     void setData(byte[] var1, int var2);

     byte[] nextSegment();

     void backward(int var1);

     void forward(int var1);
}
