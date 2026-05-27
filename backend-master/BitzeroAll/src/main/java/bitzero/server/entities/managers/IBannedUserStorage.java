package bitzero.server.entities.managers;

import java.io.IOException;

public interface IBannedUserStorage {
     void init();

     void destroy();

     BanUserData load() throws Exception;

     void save(BanUserData var1) throws IOException;
}
