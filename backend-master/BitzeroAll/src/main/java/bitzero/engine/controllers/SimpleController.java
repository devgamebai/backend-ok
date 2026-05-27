package bitzero.engine.controllers;

import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.io.IRequest;
import bitzero.engine.io.IResponse;
import bitzero.engine.io.Response;
import bitzero.engine.sessions.ISessionManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class SimpleController extends AbstractController {
     private final BitZeroEngine engine = BitZeroEngine.getInstance();
     private final ISessionManager sessionManager;

     public SimpleController() {
          this.sessionManager = this.engine.getSessionManager();
     }

     public void processRequest(IRequest request) throws Exception {
          String reqId = (String)request.getId();
          if (reqId.equals("generic")) {
               this.handleGenericRequest(request);
          } else if (reqId.equals("delay")) {
               this.handleDelayedRequest(request);
          } else if (reqId.equals("msg")) {
               if (request.getContent().equals("big")) {
                    this.handleBigMessage(request);
               } else {
                    this.handleChatMessage(request);
               }
          } else if (reqId.equals("restart")) {
               this.handleRestart();
          }

     }

     private void handleRestart() {
          BitZeroEngine.getInstance().restart();
     }

     private void handleChatMessage(IRequest request) {
          IResponse response = new Response();
          response.setId(response.getId());
          response.setContent(request.getContent());
          response.setRecipients((Collection)this.sessionManager.getAllSessions());
          response.write();
     }

     private void handleDelayedRequest(IRequest request) {
          IResponse response = new Response();
          response.setId(response.getId());
          response.setContent("** This is a delayed response **");
          String expectedDelayValue = (String)request.getContent();
          int delay = 1;
           delay = Integer.parseInt(expectedDelayValue);
          List recs = new ArrayList();
          recs.add(request.getSender());
          response.setRecipients((Collection)recs);
          response.write(delay);
     }

     private void handleGenericRequest(IRequest request) {
          IResponse response = new Response();
          response.setId(response.getId());
          response.setContent("Thanks for your generic message: " + request.getContent());
          List recs = new ArrayList();
          recs.add(request.getSender());
          response.setRecipients((Collection)recs);
          response.write();
     }

     private void handleBigMessage(IRequest request) {
          char[] rawData = new char[262144];
          Arrays.fill(rawData, 'J');
          IResponse response = new Response();
          response.setId(response.getId());
          response.setContent("Big Message: " + new String(rawData) + ", and have a nice day!");
          List recs = new ArrayList();
          recs.add(request.getSender());
          response.setRecipients((Collection)recs);
          response.write();
     }
}
