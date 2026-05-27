package bitzero.server.util.stats;

import bitzero.engine.core.BitZeroEngine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class NetworkTrafficMeter implements INetworkTrafficMeter {
     public static final int DEFAULT_MONITORED_HOURS = 24;
     public static final int DEFAULT_SAMPLING_RATE_MINUTES = 1;
     private int monitoredHours;
     private int samplingRateMinutes;
     private int samplesPerHour;
     private volatile long maxTrafficValueEverSeen;
     private volatile long minTrafficValueEverSeen;
     private volatile long lastUpdateTime;
     private LinkedList trafficDataByHour;
     private TrafficType trafficType;

     public NetworkTrafficMeter(TrafficType type) {
          this(type, 24, 1);
     }

     public NetworkTrafficMeter(TrafficType type, int monitoredHours, int samplingRateMinutes) {
          this.maxTrafficValueEverSeen = 0L;
          this.minTrafficValueEverSeen = Long.MAX_VALUE;
          this.monitoredHours = monitoredHours;
          this.samplingRateMinutes = samplingRateMinutes;
          this.trafficType = type;
          this.trafficDataByHour = new LinkedList();
          this.trafficDataByHour.add(new ArrayList());
          this.lastUpdateTime = System.currentTimeMillis();
          this.samplesPerHour = 60 / this.samplingRateMinutes;
     }

     public long getLastUpdateMillis() {
          return this.lastUpdateTime;
     }

     public List getDataPoints() {
          return this.getDataPoints(0);
     }

     public long getMaxTraffic() {
          return this.maxTrafficValueEverSeen;
     }

     public long getMinTraffic() {
          return this.minTrafficValueEverSeen;
     }

     public List getDataPoints(int howManyPoints) {
          List flatData = this.getFlatData();
          if (howManyPoints < 1) {
               return flatData;
          } else if (howManyPoints > flatData.size()) {
               return flatData;
          } else {
               int steps = flatData.size() / howManyPoints;
               List selectedDataPoints = new ArrayList();

               for(int i = 0; i < flatData.size(); i += steps) {
                    selectedDataPoints.add((Long)flatData.get(i));
               }

               return selectedDataPoints;
          }
     }

     public int getMonitoredHours() {
          return this.monitoredHours;
     }

     public int getSamplingRateMinutes() {
          return this.samplingRateMinutes;
     }

     public long getTrafficAverage() {
          List values = new ArrayList();
          synchronized(this.trafficDataByHour) {
               Iterator iterator = this.trafficDataByHour.iterator();

               while(iterator.hasNext()) {
                    List samples = (List)iterator.next();
                    synchronized(samples) {
                         values.add(this.getAverage(samples));
                    }
               }

               return this.getAverage(values);
          }
     }

     public void onTick() {
          int currHour = this.trafficDataByHour.size();
          List samples = (List)this.trafficDataByHour.get(currHour - 1);
          long trafficValue;
          if (this.trafficType == TrafficType.INCOMING) {
               trafficValue = BitZeroEngine.getInstance().getEngineReader().getReadBytes() / 1024L;
          } else {
               trafficValue = BitZeroEngine.getInstance().getEngineWriter().getWrittenBytes() / 1024L;
          }

          if (trafficValue > this.maxTrafficValueEverSeen) {
               this.maxTrafficValueEverSeen = trafficValue;
          }

          if (trafficValue < this.minTrafficValueEverSeen) {
               this.minTrafficValueEverSeen = trafficValue;
          }

          synchronized(samples) {
               samples.add(trafficValue);
          }

          if (samples.size() == this.samplesPerHour) {
               synchronized(this.trafficDataByHour) {
                    if (this.trafficDataByHour.size() == this.monitoredHours) {
                         this.trafficDataByHour.removeFirst();
                    }

                    this.trafficDataByHour.add(new ArrayList());
               }
          }

          this.lastUpdateTime = System.currentTimeMillis();
     }

     public String toString() {
          StringBuilder sb = new StringBuilder();
          synchronized(this.trafficDataByHour) {
               int hour = 1;
               Iterator iterator = this.trafficDataByHour.iterator();

               while(iterator.hasNext()) {
                    List samples = (List)iterator.next();
                    sb.append(hour++).append(" => ").append(samples.toString()).append("\n");
               }

               return sb.toString();
          }
     }

     private long getAverage(List data) {
          if (data.size() == 0) {
               return 0L;
          } else {
               long tot = 0L;

               Long value;
               for(Iterator iterator = data.iterator(); iterator.hasNext(); tot += value) {
                    value = (Long)iterator.next();
               }

               return (long)((int)tot / data.size());
          }
     }

     private List getFlatData() {
          List flatData = new ArrayList();
          synchronized(this.trafficDataByHour) {
               Iterator iterator = this.trafficDataByHour.iterator();

               while(iterator.hasNext()) {
                    List samples = (List)iterator.next();
                    synchronized(samples) {
                         flatData.addAll(samples);
                    }
               }

               return flatData;
          }
     }
}
