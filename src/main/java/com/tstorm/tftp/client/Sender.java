package com.tstorm.tftp.client;

import com.tstorm.tftp.packet.AckList;
import com.tstorm.tftp.packet.PacketGenerator;
import com.tstorm.tftp.server.SlidingWindow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

public class Sender {

    /**
     * A semaphore is used to represent the available window so that
     * we can seperate the sending and receiving logic into seperate threads
     */
    private final Semaphore window;

    private final DatagramSocket socket;
    private final InetAddress host;
    private int lastAckReceived;

    public Sender(String host) {
        try {
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(1000);
            this.host = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not locate host " + host, e);
        } catch (SocketException e) {
            throw new RuntimeException("socket err", e);
        }
        this.window = new Semaphore(SlidingWindow.WINDOW_SIZE);
    }

    /**
     * Deconstructs a file into it's packets and initalizes the sending and receiving threads
     *
     * @param filename The file path to be uploaded
     * @return reference to the receiving thread (used for syncing after completion)
     */
    public Thread upload(String filename) {
        try {
            PacketGenerator packetGenerator = PacketGenerator.createInstance(filename);
            SendingThread sender = new SendingThread(packetGenerator);
            sendFileInfo(filename, packetGenerator.totalPackets());
            Thread receiver = new Thread(new ReceivingThread(sender, packetGenerator.totalPackets()));
            receiver.start();
            new Thread(sender).start();
            return receiver;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find file " + filename, e);
        }
    }

    /**
     * Sends the file name and total number of packets to the server
     *
     * @param filepath The path of the file being uploaded
     * @param totalPackets Total packet count
     */
    private void sendFileInfo(String filepath, int totalPackets) {
        String filename = removePath(filepath);
        byte[] name = filename.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(name.length + SlidingWindow.HEADER_SIZE);
        buffer.putInt(totalPackets);
        buffer.put(name);
        try {
            this.socket.send(new DatagramPacket(buffer.array(), buffer.array().length, this.host, SlidingWindow.PORT));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Helper method to remove the path from the filename
     *
     * @param path The full path to the file
     * @return Substring from the last occurance of '/' to the end of the string
     */
    private String removePath(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Encapsulates the sending logic into a seperate thread
     */
    private class SendingThread implements Runnable {

        private final PacketGenerator packetGenerator;

        public SendingThread(PacketGenerator packetGenerator) {
            this.packetGenerator = packetGenerator;
        }

        /**
         * Sliding Window Protocol
         */
        public void run() {
            int lastFrameSent = 1;
            while (lastFrameSent < packetGenerator.totalPackets()) {
                if (window.tryAcquire()) {
                    sendFrame(lastFrameSent++);
                } else {
                    // We block here because it's only executed when a time out occurs,
                    // which hopefully doesn't happen
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        /**
         * Generate and send a {@link DatagramPacket} over the {@link DatagramSocket}, this method
         * blocks because contention is low. The only time contention occurs is when the
         * {@link ReceivingThread} has to resend a frame
         *
         * @param seqNum The sequence number for the packet to send
         */
        public synchronized void sendFrame(int seqNum) {
            try {
                byte[] frame = packetGenerator.generate(seqNum);
                socket.send(new DatagramPacket(frame, frame.length, host, SlidingWindow.PORT));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

    }

    /**
     * Encapsulates the receiving logic into a seperate thread
     */
    private class ReceivingThread implements Runnable {

        /**
         * Reference to the sending thread, only used for resending a frame after a time
         * out occurs waiting for an ACK
         */
        private final SendingThread sender;
        /**
         * Since we are only expecting one ack at a time, we can reuse the same DatagramPacket
         */
        private final DatagramPacket ack;

        private final AckList ackList;
        private final int totalPackets;

        public ReceivingThread(SendingThread sender, int totalPackets) {
            this.sender = sender;
            int size = SlidingWindow.HEADER_SIZE;
            this.ack = new DatagramPacket(new byte[size], size, host, SlidingWindow.PORT);
            this.ackList = new AckList(SlidingWindow.WINDOW_SIZE);
            this.totalPackets = totalPackets;
        }

        /**
         * Sliding Window Protocol
         */
        public void run() {
            while (lastAckReceived < totalPackets) {
                try {
                    socket.receive(this.ack);
                    int ackNum = ByteBuffer.wrap(this.ack.getData()).getInt();
                    if (ackNum == lastAckReceived + 1) {
                        // When we receive the expected ACK in order, we need to check if there are
                        // any buffered ACKs so we can slide the window the appropriate amount.
                        // The value of lastAckReceived has already been ACKed so it couldn't be buffered,
                        // also lastAckReceived + 1 just came over the wire, so it can't be buffered
                        // either. Therefore, we can start our search at lastAckReceived + 2
                        int firstPotentialBufferedAck = lastAckReceived + 2;
                        int ackCount = ackList.getBufferedAckCount(firstPotentialBufferedAck);
                        lastAckReceived += ackCount;
                        window.release(ackCount);
                    } else {
                        ackList.bufferAck(ackNum);
                    }
                } catch (SocketTimeoutException timeout) {
                    // Resend the first frame that hasn't been ACKed
                    sender.sendFrame(lastAckReceived + 1);
                    synchronized (sender) {
                        sender.notify();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }

    }

}
