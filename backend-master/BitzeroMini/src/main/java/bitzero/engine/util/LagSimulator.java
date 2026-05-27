package bitzero.engine.util;

public class LagSimulator {
     static void lagMe(int milliseconds) throws InterruptedException {
          Thread.sleep((long)milliseconds);
     }
}
