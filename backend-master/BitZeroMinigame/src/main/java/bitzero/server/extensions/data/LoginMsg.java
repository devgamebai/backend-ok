package bitzero.server.extensions.data;

import java.nio.ByteBuffer;

public class LoginMsg extends BaseMsg {
    public int id;

    public LoginMsg() {
        super((short)2);
        id = 0;
    }

    public byte[] createData() {
        ByteBuffer bf = this.makeBuffer();
        bf.putInt(this.id);
        return this.packBuffer(bf);
    }
}
