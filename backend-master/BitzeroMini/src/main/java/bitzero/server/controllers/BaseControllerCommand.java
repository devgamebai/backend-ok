package bitzero.server.controllers;

import bitzero.engine.io.IRequest;
import bitzero.engine.io.IResponse;
import bitzero.engine.io.Response;
import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.api.IBZApi;
import bitzero.server.config.DefaultConstants;
import bitzero.server.entities.User;
import bitzero.server.entities.Zone;
import bitzero.server.exceptions.BZRuntimeException;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.security.SystemPermission;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseControllerCommand implements IControllerCommand {
     public static final String KEY_SUPER_ADMIN = "SuperAdmin";
     protected final Logger logger = LoggerFactory.getLogger(this.getClass());
     protected final BitZeroServer bz = BitZeroServer.getInstance();
     protected final IBZApi api;
     private short id;
     private final SystemRequest requestType;
     private final List superUserRequest;

     public BaseControllerCommand(SystemRequest request) {
          this.superUserRequest = Arrays.asList(SystemRequest.ModeratorMessage, SystemRequest.AdminMessage);
          this.api = this.bz.getAPIManager().getBzApi();
          this.id = (Short)request.getId();
          this.requestType = request;
     }

     public Object preProcess(IRequest request) throws Exception {
          return null;
     }

     public short getId() {
          return this.id;
     }

     protected Boolean checkSuperAdmin(ISession session) {
          return session.getProperty("SuperAdmin") != null ? true : false;
     }

     protected void setAdmin(ISession session, String name) {
          session.setProperty("SuperAdmin", name);
          session.setConnected(true);
          session.setMaxLoggedInIdleTime(86400);
          session.setMaxIdleTime(86400);
     }

     protected void send(ISession sender, BaseMsg msg) {
          IResponse response = new Response();
          response.setId(this.getId());
          response.setRecipients(sender);
          response.setContent(msg.createData());
          response.setTargetController(DefaultConstants.CORE_SYSTEM_CONTROLLER_ID);
          response.write();
     }

     public SystemRequest getRequestType() {
          return this.requestType;
     }

     protected String getAdminName(ISession session) {
          return (String)session.getProperty("SuperAdmin");
     }

     protected User checkRequestPermissions(IRequest request, SystemRequest requestType) {
          User sender = this.api.getUserBySession(request.getSender());
          if (sender == null) {
               throw new BZRuntimeException(String.format("System Request rejected: %s, Client is not logged in. ", request.getSender()));
          } else if (sender.isBeingKicked()) {
               throw new BZRuntimeException(String.format("System Request rejected: %s, Client is marked for kicking. ", sender));
          } else {
               Zone zone = sender.getZone();

               try {
                    zone.getFloodFilter().filterRequest(requestType, sender);
               } catch (Exception var6) {
                    throw new BZRuntimeException();
               }

               boolean isValid = false;
               if (this.superUserRequest.contains(requestType)) {
                    isValid = zone.getPrivilegeManager().isFlagSet(sender, SystemPermission.SuperUser);
               } else {
                    isValid = zone.getPrivilegeManager().isRequestAllowed(sender, requestType);
               }

               if (!isValid) {
                    throw new BZRuntimeException(String.format("System Request rejected: insufficient privileges. Request: %s, User: %s", requestType, sender));
               } else {
                    return sender;
               }
          }
     }

     protected User checkRequestPermissions(IRequest request) {
          return this.checkRequestPermissions(request, SystemRequest.fromId(this.id));
     }

     public void executeWebsocket(IRequest request) throws Exception {
     }
}
