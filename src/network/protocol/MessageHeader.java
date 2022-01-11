/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.Buffer;
import system.Config;
import system.Utils;

/**
 *
 * @author virtu
 */
public class MessageHeader {
    public static final int MESSAGE_MAGIC = Config.getConfig().p2pMagic;
    private final String command;
    private final int payloadLength;
    private final int payloadChecksum;

    private MessageHeader(String command, int payloadLength, int payloadChecksum) {
        this.command = command;
        this.payloadLength = payloadLength;
        this.payloadChecksum = payloadChecksum;
    }
        
    public static final MessageHeader fromSource(String command, int payloadLength, int payloadChecksum) {
        return new MessageHeader(command, payloadLength, payloadChecksum);
    }
    
    public static final MessageHeader fromBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int magic = buffer.getInt();
        if (magic != MESSAGE_MAGIC)
            return null; 
        String command = new String(buffer.array(), 4, 12, Utils.ASCII);
        ((Buffer)buffer).position(16);
        int payloadLength = buffer.getInt();
        int payloadChecksum = buffer.getInt();
        return new MessageHeader(command, payloadLength, payloadChecksum);
    }

    public final byte[] array() {
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(MESSAGE_MAGIC);
        int cmdLength = command.length();
        buffer.put(command.getBytes(Utils.ASCII), 0, (cmdLength>12) ? 12 : cmdLength);
        ((Buffer)buffer).position(16);
        buffer.putInt(payloadLength);
        buffer.putInt(payloadChecksum);
        return buffer.array();
    }

    public String getCommand() {
        return command;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public int getPayloadChecksum() {
        return payloadChecksum;
    }
    
}
