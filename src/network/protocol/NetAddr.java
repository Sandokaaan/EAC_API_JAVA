/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import system.Utils;

/**
 *
 * @author Vratislav Bednařík
 */
public class NetAddr {
    private final int time;
    private final long services;
    private final InetAddress address;
    private final int port;
    private final boolean ipv4; 
    private final static byte[] IP4_PREFIX = {0,0,0,0, 0,0,0,0, 0,0,-1,-1};

    private NetAddr(int time, long services, InetAddress address, int port, boolean ipv4) {
        this.time = time;
        this.services = services;
        this.address = address;
        this.port = port;
        this.ipv4 = ipv4;
    }
    
    public static final NetAddr fromBuffer(ByteBuffer buffer, boolean withTime) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int time = withTime ? buffer.getInt() : 0;
        long services = buffer.getLong();
        byte[] rawIp = new byte[16];
        buffer.get(rawIp, 0, 16);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int port = (buffer.getShort() + 65536)%65536;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        try {
            InetAddress address = InetAddress.getByAddress(rawIp);
            return new NetAddr(time, services, address, port, address instanceof java.net.Inet4Address);
        } catch (UnknownHostException ex) {
            return null;
        }        
    }

    private static InetAddress addr4(String ip) throws UnknownHostException {
        return java.net.Inet4Address.getByName(ip);
    }

    private static InetAddress addr6(String ip) throws UnknownHostException {
        return java.net.Inet6Address.getByName(ip);
    }

    public final static NetAddr fromSource(String ip, int port, long services, boolean withTime) {
        int time = withTime ? (int)Utils.timeNow() : 0;
        try {
            boolean ipv4 = ip.contains(".");
            InetAddress address = ipv4 ? addr4(ip) : addr6(ip);
            return new NetAddr(time, services, address, port, ipv4);
        } catch (UnknownHostException ex) {
            return null;
        }        
    }
    
    public byte[] array(boolean withTime) {
        int size = withTime ? 30 : 26;
        ByteBuffer rts = ByteBuffer.allocate(size);
        rts.order(ByteOrder.LITTLE_ENDIAN);
        if (withTime)
            rts.putInt((int)Utils.timeNow());
        rts.putLong(services);
        if (ipv4)
            rts.put(IP4_PREFIX);
        rts.put(address.getAddress());
        rts.order(ByteOrder.BIG_ENDIAN);
        rts.putShort((short)port);        
        return rts.array();                    
    }

    public int getTime() {
        return time;
    }

    public long getServices() {
        return services;
    }

    public String getIp() {
        return address.getHostAddress();
    }

    public int getPort() {
        return port;
    }

    public boolean isIpv4() {
        return ipv4;
    }
    
}