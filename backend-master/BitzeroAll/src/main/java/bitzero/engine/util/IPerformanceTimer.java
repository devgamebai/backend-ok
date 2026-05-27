package bitzero.engine.util;

public interface IPerformanceTimer {
     void startSampling();

     void stopSampling();

     double getAverageMillis();

     int getMaxSamples();
}
