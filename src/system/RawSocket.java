/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 *
 * @author virtu
 */
public class RawSocket {
    private final Socket socket;

    public RawSocket() {
        socket = new Socket();     
    }
    
    public boolean isOpen() {
        return ( (socket.isConnected()) && (!socket.isClosed()) );
    }
    
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public String getLocalIp() {
        return socket.getLocalAddress().getHostAddress();
    }

    public int getRemotePort() {
        return socket.getPort();
    }
    
    public String getRemoteIp() {
        return socket.getInetAddress().getHostAddress();
    }

    
    public RawSocket(Socket socket) throws SocketException {
        this.socket = socket;
        this.socket.setSoTimeout(5000);
    }
    
    public void setTimeOut(int time) throws SocketException {
        this.socket.setSoTimeout(time);
    }

    public void connect(String ip, int port) throws IOException {
        socket.connect(new InetSocketAddress(ip, port));
    }
    
    public void send(String s) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(s.getBytes(Utils.CHARSET));
    }

    private void sendBinary(byte[] binaryData) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(binaryData);
    }
        
    public String receiveTextLine() throws IOException {
        InputStream inputStream = socket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        return reader.readLine();
    }
    
    public void close() throws IOException {
        socket.close();
    }
    
    public ByteArrayOutputStream receivePacket(boolean wait) throws IOException {
        InputStream inputStream = socket.getInputStream();
        final int MAXSIZE = 65536;
        byte[] buffer = new byte[MAXSIZE];
        ByteArrayOutputStream rts = new ByteArrayOutputStream();
        while (true) {
            int nRead = inputStream.read(buffer, 0, MAXSIZE);
            //System.err.println("receivePacket: " + nRead);
            if (nRead > 0)
                rts.write(buffer, 0, nRead);
            else 
                break;
            if ((!wait)&&(inputStream.available() <=0 ))
                break;
        }
        //System.err.println("receivePacket: end of packet " + rts.size());
        return rts;
    }
    
    public ByteBuffer receiveBytes(int length) throws IOException {
        int totalRead = 0;
        if (length >= 0) {                       
            ByteBuffer byteBuffer = ByteBuffer.allocate(length);
            InputStream inputStream = socket.getInputStream();
            byte[] rtsInnerBuffer = byteBuffer.array();
            while (totalRead < length) {
                int nRead = inputStream.read(rtsInnerBuffer, totalRead, (length-totalRead));
                if (nRead > 0)
                    totalRead += nRead;
                else 
                    break;
            }
            if (totalRead == length)
                return byteBuffer;
            System.err.println("rs:" + totalRead + "/" + length + ": " + Utils.getAsHex(rtsInnerBuffer));
        }
        return null;
    }

}
