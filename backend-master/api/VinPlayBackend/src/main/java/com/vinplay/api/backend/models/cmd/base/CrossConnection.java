package com.vinplay.api.backend.models.cmd.base;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class CrossConnection {
     private Socket socket = null;
     private BufferedOutputStream bos;
     private BufferedInputStream bis;

     public CrossConnection(String ip, int port) throws IOException {
          SocketAddress addr = new InetSocketAddress(ip, port);
          this.socket = new Socket();
          this.socket.setSoTimeout(500);
          this.socket.connect(addr, 100);
          this.bos = new BufferedOutputStream(this.socket.getOutputStream());
          this.bis = new BufferedInputStream(this.socket.getInputStream());
     }

     public void send(byte[] data) throws IOException {
          this.bos.write(data);
          this.bos.flush();
     }

     public int read(byte[] buffer) throws IOException {
          return this.bis.read(buffer, 0, buffer.length);
     }

     public void close() {
          if (this.socket != null) {
               try {
                    this.socket.close();
               } catch (IOException var2) {
               }
          }

     }
}
