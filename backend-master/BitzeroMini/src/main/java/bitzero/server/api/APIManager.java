package bitzero.server.api;

import bitzero.engine.service.IService;
import bitzero.server.BitZeroServer;
import bitzero.server.api.response.ResponseApi;

public class APIManager implements IService {
     private final String serviceName = "APIManager";
     private BitZeroServer bz;
     private ResponseApi resApi;
     private IBZApi bzApi;

     public void init(Object o) {
          this.bz = BitZeroServer.getInstance();
          this.resApi = new ResponseApi();
          this.bzApi = new BZApi(this.bz);
     }

     public IBZApi getBzApi() {
          return this.bzApi;
     }

     public ResponseApi getResApi() {
          return this.resApi;
     }

     public void destroy(Object obj) {
     }

     public String getName() {
          return "APIManager";
     }

     public void handleMessage(Object msg) {
          throw new UnsupportedOperationException("Not supported");
     }

     public void setName(String arg0) {
          throw new UnsupportedOperationException("Not supported");
     }
}
