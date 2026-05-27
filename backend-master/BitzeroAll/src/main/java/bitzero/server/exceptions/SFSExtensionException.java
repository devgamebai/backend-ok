package bitzero.server.exceptions;

public class SFSExtensionException extends SFSException {
     public SFSExtensionException() {
     }

     public SFSExtensionException(String message) {
          super(message);
     }

     public SFSExtensionException(String message, SFSErrorData data) {
          super(message, data);
     }
}
