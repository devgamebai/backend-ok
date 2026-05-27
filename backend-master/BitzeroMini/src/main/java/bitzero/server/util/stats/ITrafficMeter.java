package bitzero.server.util.stats;

import java.util.List;

public interface ITrafficMeter {
     int getMonitoredHours();

     int getSamplingRateMinutes();

     int getTrafficAverage();

     int getTrafficAverage(int var1);

     int getMaxTraffic();

     int getMinTraffic();

     List getDataPoints();

     List getDataPoints(int var1);

     long getLastUpdateMillis();

     void onTick();
}
