package bitzero.server.exceptions;

public class BZExtensionException extends BZException {
     public BZExtensionException() {
     }

     public BZExtensionException(String message) {
          super(message);
     }

     public BZExtensionException(String message, BZErrorData data) {
          super(message, data);
     }
}
