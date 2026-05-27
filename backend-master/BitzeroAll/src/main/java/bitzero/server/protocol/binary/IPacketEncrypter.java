package bitzero.server.protocol.binary;

import bitzero.engine.sessions.ISession;

public interface IPacketEncrypter {
     byte[] encrypt(ISession var1, byte[] var2);

     byte[] decrypt(ISession var1, byte[] var2);
}
