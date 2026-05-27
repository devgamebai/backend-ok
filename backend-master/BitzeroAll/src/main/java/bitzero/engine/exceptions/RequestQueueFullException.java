package bitzero.engine.exceptions;

public class RequestQueueFullException extends Exception {
     public RequestQueueFullException() {
     }

     public RequestQueueFullException(String message) {
          super(message);
     }
}
