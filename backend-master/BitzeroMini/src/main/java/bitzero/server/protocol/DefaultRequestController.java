package bitzero.server.protocol;

import bitzero.engine.controllers.AbstractController;
import bitzero.engine.io.IRequest;
import bitzero.engine.io.IResponse;
import bitzero.engine.io.Response;
import bitzero.engine.sessions.ISession;
import java.util.ArrayList;
import java.util.List;

public class DefaultRequestController extends AbstractController {
     public void processRequest(IRequest request) throws Exception {
     }

     private void handleReqOne(IRequest request) {
     }

     private void sendResponse(short actionId, Object recipients, Object responseObject) {
          List recipientList = null;
          if (recipients instanceof List) {
               recipientList = (List)recipients;
          } else {
               if (!(recipients instanceof ISession)) {
                    throw new IllegalArgumentException("Wrong recipients argument in sendResponse!");
               }

                recipientList = new ArrayList();
               recipientList.add((ISession)recipients);
          }

          IResponse response = new Response();
          response.setId(this.id);
          response.write();
     }
}
