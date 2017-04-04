package com.tstorm.tftp.server;

import com.tstorm.tftp.packet.AckList;

import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class Receiver extends SlidingWindow implements Runnable {

    private final AckList ackList;
    private final FileAssembler fileAssembler;
    private int largestAcceptableFrame, lastFrameReceived, lastFrameSeqNum;

    public Receiver() throws SocketException, FileNotFoundException {
        super(PORT);
        this.ackList = new AckList(WINDOW_SIZE);
        this.largestAcceptableFrame = WINDOW_SIZE;
        this.fileAssembler = new FileAssembler();
        this.fileAssembler.start();
    }

    public void run() {
        boolean finalPacketReceived = false;
        receiveFileInfo();
        while (!finalPacketReceived) {
            byte[] frame = new byte[HEADER_SIZE + BLOCK_SIZE];
            DatagramPacket packet = new DatagramPacket(frame, frame.length);
            receive(packet);
            int seqNum = ByteBuffer.wrap(frame).getInt();
            if (lastFrameReceived < seqNum) {
                if (seqNum <= largestAcceptableFrame) {
                    fileAssembler.acceptFrame(packet);
                    if (seqNum == lastFrameReceived + 1) {
                        int ackCount = ackList.getBufferedAckCount(lastFrameReceived);
                        lastFrameReceived += ackCount;
                        largestAcceptableFrame = lastFrameReceived + WINDOW_SIZE;
                    } else {
                        ackList.bufferAck(seqNum);
                    }
                    send(createAck(seqNum, packet.getAddress(), packet.getPort()));
                }
            } else {
                // the ack was dropped
                send(createAck(seqNum, packet.getAddress(), packet.getPort()));
            }
            finalPacketReceived = lastFrameReceived == lastFrameSeqNum;
        }
        fileAssembler.flushBuffer().interrupt();
    }

    private void receiveFileInfo() {
        byte[] info = new byte[BLOCK_SIZE];
        DatagramPacket fileInfo = new DatagramPacket(info, info.length);
        receive(fileInfo);
        ByteBuffer buffer = ByteBuffer.wrap(fileInfo.getData());
        this.lastFrameSeqNum = buffer.getInt();
        byte[] filename = new byte[fileInfo.getLength() - HEADER_SIZE];
        buffer.get(filename, 0, filename.length);
        this.fileAssembler.setFileName(new String(filename));
    }

    private DatagramPacket createAck(int seqNum, InetAddress address, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(SlidingWindow.HEADER_SIZE);
        buffer.putInt(seqNum);
        byte[] payload = buffer.array();
        buffer.flip();
        return new DatagramPacket(payload, HEADER_SIZE, address, port);
    }

}
