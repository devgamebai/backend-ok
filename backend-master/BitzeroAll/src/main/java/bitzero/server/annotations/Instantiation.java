package bitzero.server.annotations;

import java.lang.annotation.Annotation;

public interface Instantiation extends Annotation {
     InstantiationMode value();

     public static enum InstantiationMode {
          NEW_INSTANCE,
          SINGLE_INSTANCE;
     }
}
