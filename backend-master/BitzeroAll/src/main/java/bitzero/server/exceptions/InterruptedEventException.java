package bitzero.server.exceptions;

public final class InterruptedEventException extends BZRuntimeException {
     private static final long serialVersionUID = 1729674312557697005L;

     public InterruptedEventException() {
          super("Event Interrupted");
     }
}
