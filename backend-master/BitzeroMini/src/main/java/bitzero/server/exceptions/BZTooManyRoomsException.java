package bitzero.server.exceptions;

public class BZTooManyRoomsException extends BZException {
     public BZTooManyRoomsException(String message) {
          super(message);
     }

     public BZTooManyRoomsException(String message, BZErrorData data) {
          super(message, data);
     }
}
