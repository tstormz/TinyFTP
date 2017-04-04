package com.tstorm.tftp.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.concurrent.Exchanger;

public class FileAssembler extends Thread {

    private RandomAccessFile file;
    private final PacketBuffer<DatagramPacket> bufferA, bufferB;
    private final Exchanger<PacketBuffer> exchanger;
    private boolean toggle = false;
    private volatile boolean isRunning = true;

    public FileAssembler() {
        this.bufferA = new PacketBuffer<>(SlidingWindow.WINDOW_SIZE);
        this.bufferB = new PacketBuffer<>(SlidingWindow.WINDOW_SIZE);
        this.exchanger = new Exchanger<>();
    }

    public void setFileName(String filename) {
        try {
            this.file = new RandomAccessFile(filename, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void acceptFrame(DatagramPacket packet) {
        PacketBuffer<DatagramPacket> buffer = toggle ? bufferA : bufferB;
        buffer.add(packet);
        if (buffer.isFull()) {
            try {
                exchanger.exchange(buffer);
                this.toggle = !this.toggle;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public FileAssembler flushBuffer() {
        while (!bufferA.isEmpty()) {
            extractPayload(bufferA.poll());
        }
        while(!bufferB.isEmpty()) {
            extractPayload(bufferB.poll());
        }
        this.isRunning = false;
        return this;
    }

    @Override
    public void run() {
        PacketBuffer<DatagramPacket> buffer = bufferA;
        while (isRunning) {
            try {
                if (buffer.isEmpty()) {
                    buffer = exchanger.exchange(buffer);
                }
                extractPayload(buffer.poll());
            } catch (InterruptedException interrupt) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    private void extractPayload(DatagramPacket packet) {
        byte[] frame = packet.getData();
        int length = packet.getLength() - SlidingWindow.HEADER_SIZE;
        int seqNum = ByteBuffer.wrap(frame).getInt();
        try {
            file.seek((seqNum - 1) * SlidingWindow.BLOCK_SIZE);
            file.write(frame, SlidingWindow.HEADER_SIZE, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

