package bitzero.server.exceptions;

public class BZLoginException extends BZException {
     public BZLoginException() {
     }

     public BZLoginException(String message) {
          super(message);
     }

     public BZLoginException(String message, BZErrorData data) {
          super(message, data);
     }
}
