package bitzero.engine.io;

import bitzero.engine.io.filter.IFilterChain;

public abstract class AbstractIOHandler implements IOHandler {
     protected IProtocolCodec codec;
     protected volatile long readPackets;
     protected IFilterChain preFilterChain;
     protected IFilterChain postFilterChain;

     public IProtocolCodec getCodec() {
          return this.codec;
     }

     public void setCodec(IProtocolCodec codec) {
          this.codec = codec;
     }

     public long getReadPackets() {
          return this.readPackets;
     }

     public IFilterChain getPreFilterChain() {
          return this.preFilterChain;
     }

     public void setPreFilterChain(IFilterChain preFilterChain) {
          this.preFilterChain = preFilterChain;
     }

     public IFilterChain getPostFilterChain() {
          return this.postFilterChain;
     }

     public void setPostFilterChain(IFilterChain postFilterChain) {
          this.postFilterChain = postFilterChain;
     }
}
