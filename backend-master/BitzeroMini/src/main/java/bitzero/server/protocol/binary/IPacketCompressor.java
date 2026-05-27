package bitzero.server.protocol.binary;

public interface IPacketCompressor {
     byte[] compress(byte[] var1) throws Exception;

     byte[] uncompress(byte[] var1) throws Exception;
}
