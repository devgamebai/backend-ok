package bitzero.server.exceptions;

public class BZFloodingException extends BZException {
     public BZFloodingException() {
     }

     public BZFloodingException(String message) {
          super(message);
     }

     public BZFloodingException(String message, BZErrorData data) {
          super(message, data);
     }
}
