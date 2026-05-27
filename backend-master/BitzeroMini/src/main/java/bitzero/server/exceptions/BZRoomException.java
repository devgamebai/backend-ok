package bitzero.server.exceptions;

public class BZRoomException extends BZException {
     public BZRoomException() {
     }

     public BZRoomException(String message) {
          super(message);
     }

     public BZRoomException(String message, BZErrorData data) {
          super(message, data);
     }
}
