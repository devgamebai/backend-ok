package bitzero.server.entities.managers;

import bitzero.server.config.ZoneSettings;
import bitzero.server.core.ICoreService;
import bitzero.server.entities.Zone;
import bitzero.server.exceptions.BZException;
import java.util.List;

public interface IZoneManager extends ICoreService {
     List getZoneList();

     Zone getZoneByName(String var1);

     Zone getZoneById(int var1);

     void initializeZones() throws BZException;

     void addZone(Zone var1);

     void toggleZone(String var1, boolean var2);

     Zone createZone(ZoneSettings var1) throws BZException;
}
