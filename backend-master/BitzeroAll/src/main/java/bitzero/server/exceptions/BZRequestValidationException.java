package bitzero.server.exceptions;

public class BZRequestValidationException extends Exception {
     public BZRequestValidationException() {
     }

     public BZRequestValidationException(String message) {
          super(message);
     }

     public BZRequestValidationException(Throwable t) {
          super(t);
     }
}
