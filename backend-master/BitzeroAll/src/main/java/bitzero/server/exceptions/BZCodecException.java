package bitzero.server.exceptions;

public class BZCodecException extends BZException {
     public BZCodecException() {
     }

     public BZCodecException(String message) {
          super(message);
     }

     public BZCodecException(Throwable t) {
          super(t);
     }
}
