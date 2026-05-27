package com.vinplay.api.backend.models.cmd.base;

public class BZException extends Exception {
     private static final long serialVersionUID = 6052949605652105170L;
     BZErrorData errorData;

     public BZException() {
          this.errorData = null;
     }

     public BZException(String message) {
          super(message);
          this.errorData = null;
     }

     public BZException(String message, BZErrorData data) {
          super(message);
          this.errorData = data;
     }

     public BZException(Throwable t) {
          super(t);
          this.errorData = null;
     }

     public BZErrorData getErrorData() {
          return this.errorData;
     }
}
