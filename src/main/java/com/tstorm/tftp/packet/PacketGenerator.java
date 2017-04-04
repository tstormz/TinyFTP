package com.tstorm.tftp.packet;

import com.tstorm.tftp.server.SlidingWindow;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

/**
 * Splits up a file into frames
 */
public class PacketGenerator {

    private static PacketGenerator instance;
    private final Packet[] file;

    private PacketGenerator(String filename) throws FileNotFoundException {
        RealPacket.setFile(filename);
        long filesize = new File(filename).length();
        int numberOfPackets = (int) filesize / SlidingWindow.BLOCK_SIZE;
        int finalPacketSize = (int) filesize % SlidingWindow.BLOCK_SIZE;
        // If the file size just happens to be divisible by the block size,
        // then there is no final packet
        numberOfPackets += (finalPacketSize == 0) ? 0 : 1;
        this.file = new ProxyPacket[numberOfPackets];
        for (int i = 0; i < numberOfPackets - 1; i++) {
            this.file[i] = new ProxyPacket(i, SlidingWindow.BLOCK_SIZE);
        }
        if (finalPacketSize != 0) {
            this.file[numberOfPackets - 1] = new ProxyPacket(numberOfPackets - 1, finalPacketSize);
        }
    }

    public static PacketGenerator createInstance(String filename) throws FileNotFoundException {
        if (instance == null) {
            return new PacketGenerator(filename);
        } else {
            System.err.printf("Warning: PacketGenerator singleton already exists, '%s' was not created\n", filename);
            return instance;
        }
    }

    public byte[] generate(int seqNum) {
        byte[] payload = this.file[seqNum - 1].createPayload();
        ByteBuffer frame = ByteBuffer.wrap(payload);
        frame.putInt(seqNum);
        // if we are on the last packet, close the file because files should only be generated once
        if (seqNum == file.length) {
            RealPacket.closeFile();
        }
        return frame.array();
    }

    public int totalPackets() {
        return file.length;
    }
}
