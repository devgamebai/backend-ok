package bitzero.engine.core.security;

import bitzero.engine.exceptions.RefusedAddressException;

public interface IConnectionFilter {
     void addBannedAddress(String var1);

     void removeBannedAddress(String var1);

     String[] getBannedAddresses();

     void validateAndAddAddress(String var1) throws RefusedAddressException;

     String getGhostList();

     void removeAddress(String var1);

     void addWhiteListAddress(String var1);

     void removeWhiteListAddress(String var1);

     String[] getWhiteListAddresses();

     int getMaxConnectionsPerIp();

     void setMaxConnectionsPerIp(int var1);
}
