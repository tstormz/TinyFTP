package com.tstorm.tftp.packet;

import com.tstorm.tftp.server.SlidingWindow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Constructed from the {@link ProxyPacket}, this is the object
 * that actually contains the payload.
 */
public class RealPacket implements Packet {

    private static RandomAccessFile file;
    private final int id, blockSize;

    public static void setFile(String filename) throws FileNotFoundException {
        file = new RandomAccessFile(filename, "r");
    }

    protected static void closeFile() {
        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to close file", e);
        }
    }

    public RealPacket(int id, int size) {
        this.id = id;
        this.blockSize = size;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] createPayload() {
        byte[] frame = new byte[blockSize + SlidingWindow.HEADER_SIZE];
        try {
            file.seek(id * SlidingWindow.BLOCK_SIZE);
            ByteBuffer.wrap(frame).putInt(this.id);
            file.read(frame, SlidingWindow.HEADER_SIZE, blockSize);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return frame;
    }

}
