package bitzero.server.util.stats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ZoneTrafficMeter implements ITrafficMeter {
     private static final int DEFAULT_MONITORED_HOURS = 24;
     private static final int DEFAULT_SAMPLING_RATE_MINUTES = 5;
     private int monitoredHours;
     private int samplingRateMinutes;
     private int samplesPerHour;
     private int maxTrafficValueEverSeen;
     private int minTrafficValueEverSeen;
     private volatile long lastUpdateTime;
     LinkedList trafficDataByHour;

     public ZoneTrafficMeter() {
          this(24, 5);
     }

     public ZoneTrafficMeter(int monitoredHours, int samplingRateMinutes) {
          this.maxTrafficValueEverSeen = 0;
          this.minTrafficValueEverSeen = Integer.MAX_VALUE;
          this.monitoredHours = monitoredHours;
          this.samplingRateMinutes = samplingRateMinutes;
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
                    selectedDataPoints.add((Integer)flatData.get(i));
               }

               return selectedDataPoints;
          }
     }

     public int getMaxTraffic() {
          return this.maxTrafficValueEverSeen;
     }

     public int getMinTraffic() {
          return this.minTrafficValueEverSeen;
     }

     public int getMonitoredHours() {
          return this.monitoredHours;
     }

     public int getSamplingRateMinutes() {
          return this.samplingRateMinutes;
     }

     public int getTrafficAverage() {
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

     public int getTrafficAverage(int previousHours) {
          throw new UnsupportedOperationException("Not implemented yet");
     }

     public void onTick() {
          int currHour = this.trafficDataByHour.size();
          List samples = (List)this.trafficDataByHour.get(currHour - 1);
          int userCountNow = 1;
          if (userCountNow > this.maxTrafficValueEverSeen) {
               this.maxTrafficValueEverSeen = userCountNow;
          }

          if (userCountNow < this.minTrafficValueEverSeen) {
               this.minTrafficValueEverSeen = userCountNow;
          }

          synchronized(samples) {
               samples.add(Integer.valueOf(userCountNow));
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

     private int getAverage(List data) {
          if (data.size() == 0) {
               return 0;
          } else {
               long tot = 0L;

               Integer value;
               for(Iterator iterator = data.iterator(); iterator.hasNext(); tot += (long)value) {
                    value = (Integer)iterator.next();
               }

               return (int)tot / data.size();
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
