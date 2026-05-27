package bitzero.engine.data;

import bitzero.engine.sessions.ISession;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Packet implements IPacket {
     protected Short id = Short.valueOf((short)0);
     protected long creationTime = System.nanoTime();
     protected Object data;
     protected String ownerNode;
     protected MessagePriority priority;
     protected ISession sender;
     protected TransportType transportType;
     protected int originalSize = -1;
     protected ConcurrentMap attributes;
     protected Collection recipients;
     protected byte[] fragmentBuffer;

     public Packet() {
          this.priority = MessagePriority.NORMAL;
          this.transportType = TransportType.TCP;
     }

     public Object getAttribute(String key) {
          return this.attributes == null ? null : this.attributes.get(key);
     }

     public void setAttribute(String key, Object attr) {
          if (this.attributes == null) {
               this.attributes = new ConcurrentHashMap();
          }

          this.attributes.put(key, attr);
     }

     public long getCreationTime() {
          return this.creationTime;
     }

     public void setCreationTime(long creationTime) {
          this.creationTime = creationTime;
     }

     public Object getData() {
          return this.data;
     }

     public void setData(Object data) {
          this.data = data;
     }

     public boolean isFragmented() {
          return this.fragmentBuffer != null;
     }

     public byte[] getFragmentBuffer() {
          return this.fragmentBuffer;
     }

     public void setFragmentBuffer(byte[] bb) {
          this.fragmentBuffer = bb;
     }

     private byte[] cloneData(Object data) {
          if (data instanceof byte[]) {
               byte[] newData = new byte[((byte[])((byte[])data)).length];
               System.arraycopy(data, 0, newData, 0, newData.length);
               return newData;
          } else {
               return null;
          }
     }

     public String getOwnerNode() {
          return this.ownerNode;
     }

     public void setOwnerNode(String ownerNode) {
          this.ownerNode = ownerNode;
     }

     public MessagePriority getPriority() {
          return this.priority;
     }

     public void setPriority(MessagePriority priority) {
          this.priority = priority;
     }

     public ISession getSender() {
          return this.sender;
     }

     public void setSender(ISession sender) {
          this.sender = sender;
     }

     public TransportType getTransportType() {
          return this.transportType;
     }

     public void setTransportType(TransportType transportType) {
          this.transportType = transportType;
     }

     public Collection getRecipients() {
          return this.recipients;
     }

     public void setRecipients(Collection recipients) {
          this.recipients = recipients;
     }

     public boolean isTcp() {
          return this.transportType == TransportType.TCP;
     }

     public boolean isUdp() {
          return this.transportType == TransportType.UDP;
     }

     public int getOriginalSize() {
          return this.originalSize;
     }

     public void setOriginalSize(int originalSize) {
          if (this.originalSize == -1) {
               this.originalSize = originalSize;
          }

     }

     public String toString() {
          return String.format("{ Packet: %s, data: %s, Pri: %s }", this.transportType, this.data.getClass().getName(), this.priority);
     }

     public IPacket clone() {
          IPacket newPacket = new Packet();
          newPacket.setCreationTime(this.getCreationTime());
          newPacket.setId(this.getId());
          newPacket.setData(this.getData());
          newPacket.setOriginalSize(this.getOriginalSize());
          newPacket.setOwnerNode(this.getOwnerNode());
          newPacket.setPriority(this.getPriority());
          newPacket.setRecipients((Collection)null);
          newPacket.setSender(this.getSender());
          newPacket.setTransportType(this.getTransportType());
          return newPacket;
     }

     public Short getId() {
          return this.id;
     }

     public void setId(Short _id) {
          this.id = _id;
     }
}
