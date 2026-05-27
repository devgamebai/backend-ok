package bitzero.server.entities.managers;

import bitzero.server.BitZeroServer;
import bitzero.server.config.IConfigurator;
import bitzero.server.config.ZoneSettings;
import bitzero.server.core.BaseCoreService;
import bitzero.server.entities.Zone;
import bitzero.server.exceptions.BZException;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.util.stats.ITrafficMeter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BZZoneManager extends BaseCoreService implements IZoneManager {
     private Logger logger = LoggerFactory.getLogger(this.getClass());
     private ConcurrentMap zones;
     private BitZeroServer bz;
     private IConfigurator configurator;
     private final ConcurrentMap trafficMonitors = new ConcurrentHashMap();
     private final TrafficMeterExecutor trafficMeterExecutor;

     public Zone getZoneByName(String s) {
          return null;
     }

     public BZZoneManager() {
          if (this.zones == null) {
               this.zones = new ConcurrentHashMap();
          }

          this.trafficMeterExecutor = new TrafficMeterExecutor(this.trafficMonitors.values());
          Runtime.getRuntime().addShutdownHook(new ShutDownHandler((ShutDownHandler)null));
     }

     public void init(Object o) {
          super.init(o);
          this.bz = BitZeroServer.getInstance();
          this.configurator = this.bz.getConfigurator();
     }

     public void addZone(Zone zone) {
          if (this.zones.containsKey(zone.getId())) {
               throw new BZRuntimeException("Zone already exists: " + zone.getName() + ". Can't add the same zone more than once.");
          } else {
               this.zones.put(zone.getId(), zone);
          }
     }

     public Zone getZoneById(int id) {
          return (Zone)this.zones.get(id);
     }

     public List getZoneList() {
          return new ArrayList(this.zones.values());
     }

     public synchronized void initializeZones() throws BZException {
          if (this.zones.size() > 0) {
               this.logger.info(this.zones.size() + " Zones found in cluster: ");
               Iterator iterator = this.zones.values().iterator();

               while(iterator.hasNext()) {
                    Zone zone = (Zone)iterator.next();
                    this.logger.info(zone.toString());
               }

          }
     }

     public void toggleZone(String name, boolean isActive) {
          Zone theZone = this.getZoneByName(name);
          theZone.setActive(isActive);
     }

     public ITrafficMeter getZoneTrafficMeter(String zoneName) {
          return (ITrafficMeter)this.trafficMonitors.get(zoneName);
     }

     private void activateTrafficMonitors() {
     }

     public Zone createZone(ZoneSettings settings) throws BZException {
          Zone zone = new Zone(settings.name, settings.id);
          zone.setCustomLogin(settings.isCustomLogin);
          zone.setMaxAllowedRooms(settings.maxRooms);
          zone.setMaxAllowedUsers(settings.maxUsers);
          zone.setMinRoomNameChars(settings.minRoomNameChars);
          zone.setMaxRoomNameChars(settings.maxRoomNameChars);
          zone.setDefaultPlayerIdGeneratorClassName(settings.defaultPlayerIdGeneratorClass);
          zone.setUserCountChangeUpdateInterval(settings.userCountChangeUpdateInterval);
          zone.setUserReconnectionSeconds(settings.userReconnectionSeconds);
          zone.setMaxUserIdleTime(0);
          List defaultRoomGroups = new ArrayList();
          if (defaultRoomGroups.size() == 0) {
               defaultRoomGroups.add("default");
          }

          zone.setDefaultGroups(defaultRoomGroups);
          List publicRoomGroups = new ArrayList();
          if (publicRoomGroups.size() == 0) {
               publicRoomGroups.add("default");
          }

          zone.setPublicGroups(publicRoomGroups);
          zone.setZoneManager(this);
          zone.setActive(true);
          this.addZone(zone);
          return zone;
     }

     private void populateTransientFields() {
          this.logger = LoggerFactory.getLogger(this.getClass());
          this.bz = BitZeroServer.getInstance();
          this.configurator = this.bz.getConfigurator();
     }

     private static class TrafficMeterExecutor implements Runnable {
          private final Logger log = LoggerFactory.getLogger(this.getClass());
          private final Collection trafficMonitors;

          public void run() {
               try {
                    long t1 = System.nanoTime();
                    Iterator iterator = this.trafficMonitors.iterator();

                    while(iterator.hasNext()) {
                         ITrafficMeter monitor = (ITrafficMeter)iterator.next();
                         monitor.onTick();
                    }

                    long t2 = System.nanoTime();
                    this.log.debug("Traffic Monitor update: " + (double)(t2 - t1) / 1000000.0D + "ms.");
               } catch (Exception var6) {
                    this.log.warn("Unexpected exception: " + var6 + ". Task will not be interrupted.");
               }

          }

          public TrafficMeterExecutor(Collection trafficMonitors) {
               this.trafficMonitors = trafficMonitors;
          }
     }

     private final class ShutDownHandler extends Thread {
          public void run() {
          }

          private ShutDownHandler() {
          }

          ShutDownHandler(ShutDownHandler shutdownhandler) {
               this();
          }
     }
}
