package bitzero.server.util.stats;

import java.util.List;

public interface INetworkTrafficMeter {
     int getMonitoredHours();

     int getSamplingRateMinutes();

     long getTrafficAverage();

     long getMaxTraffic();

     long getMinTraffic();

     List getDataPoints();

     List getDataPoints(int var1);

     long getLastUpdateMillis();

     void onTick();
}
