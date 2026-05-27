package bitzero.server.config;

import bitzero.server.exceptions.BZException;
import java.io.IOException;
import java.util.List;

public interface IConfigurator {
     void loadConfiguration() throws Exception;

     List loadZonesConfiguration() throws BZException;

     void saveServerSettings(boolean var1) throws IOException;

     void saveZoneSettings(ZoneSettings var1, boolean var2) throws IOException;

     void saveZoneSettings(ZoneSettings var1, boolean var2, String var3) throws IOException;

     CoreSettings getCoreSettings();

     ServerSettings getServerSettings();

     List getZoneSettings();

     ZoneSettings getZoneSetting(String var1);

     ZoneSettings getZoneSetting(int var1);

     void saveNewZoneSettings(ZoneSettings var1) throws IOException;

     void removeZoneSetting(String var1) throws IOException;
}
