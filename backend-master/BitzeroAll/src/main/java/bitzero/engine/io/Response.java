package bitzero.engine.io;

import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.data.TransportType;
import bitzero.engine.sessions.ISession;
import bitzero.engine.util.scheduling.Scheduler;
import bitzero.engine.util.scheduling.Task;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Response extends AbstractEngineMessage implements IResponse {
     private Collection recipients;
     private TransportType type;
     private Object targetController;

     public Response() {
          this.type = TransportType.TCP;
     }

     public Collection getRecipients() {
          return this.recipients;
     }

     public TransportType getTransportType() {
          return this.type;
     }

     public boolean isTCP() {
          return this.type == TransportType.TCP;
     }

     public boolean isUDP() {
          return this.type == TransportType.UDP;
     }

     public void setRecipients(Collection recipents) {
          this.recipients = recipents;
     }

     public void setRecipients(ISession session) {
          List recipients = new ArrayList();
          recipients.add(session);
          this.setRecipients((Collection)recipients);
     }

     public void setTransportType(TransportType type) {
          this.type = type;
     }

     public void write() {
          BitZeroEngine.getInstance().write(this);
     }

     public void write(int delay) {
          Scheduler scheduler = (Scheduler)BitZeroEngine.getInstance().getServiceByName("scheduler");
          Task delayedSocketWriteTask = new Task("delayedSocketWrite");
          delayedSocketWriteTask.getParameters().put("response", this);
          scheduler.addScheduledTask(delayedSocketWriteTask, delay, false, BitZeroEngine.getInstance().getEngineDelayedTaskHandler());
     }

     public Object getTargetController() {
          return this.targetController;
     }

     public void setTargetController(Object o) {
          this.targetController = o;
     }

     public static IResponse clone(IResponse original) {
          IResponse newResponse = new Response();
          newResponse.setContent(original.getContent());
          newResponse.setTargetController(original.getTargetController());
          newResponse.setId(original.getId());
          newResponse.setTransportType(original.getTransportType());
          return newResponse;
     }
}
