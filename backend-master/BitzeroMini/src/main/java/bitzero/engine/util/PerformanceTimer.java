package bitzero.engine.util;

import java.util.Iterator;
import java.util.LinkedList;

public class PerformanceTimer implements IPerformanceTimer {
     private int maxSamples;
     private final LinkedList samples;
     private boolean started;
     private volatile long startTime;

     public PerformanceTimer() {
          this(10);
     }

     public PerformanceTimer(int maxSamples) {
          this.maxSamples = 10;
          this.started = false;
          this.maxSamples = maxSamples;
          this.samples = new LinkedList();
     }

     public double getAverageMillis() {
          long total = 0L;

          Long sample;
          for(Iterator iterator = this.samples.iterator(); iterator.hasNext(); total += sample) {
               sample = (Long)iterator.next();
          }

          return (double)(total / (long)this.samples.size()) / 1000000.0D;
     }

     public int getMaxSamples() {
          return this.maxSamples;
     }

     public void startSampling() {
          if (this.started) {
               throw new IllegalStateException("Timer already started!");
          } else {
               this.started = true;
               this.startTime = System.nanoTime();
          }
     }

     public void stopSampling() {
          if (!this.started) {
               throw new IllegalStateException("Timer is already stopped!");
          } else {
               this.started = false;
               this.addSample(System.nanoTime() - this.startTime);
          }
     }

     private void addSample(long sample) {
          if (this.samples.size() >= this.maxSamples) {
               this.samples.removeFirst();
          }

          this.samples.add(sample);
     }
}
