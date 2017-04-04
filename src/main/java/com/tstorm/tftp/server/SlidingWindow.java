package com.tstorm.tftp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class SlidingWindow extends DatagramSocket {

    public static final int PORT = 2617;
    public static final int HEADER_SIZE = 4;
    public static final int BLOCK_SIZE = 508;
    public static final int WINDOW_SIZE = 32; // must be a power of 2

    public SlidingWindow(int port) throws SocketException {
        super(port);
    }

    /**
     * Helper method to handle try/catch of exception
     *
     * @param packet {@link DatagramPacket} passed to {@link DatagramSocket#receive}
     */
    public void receive(DatagramPacket packet) {
        try {
            super.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Helper method to handle try/catch of exception
     *
     * @param packet {@link DatagramPacket} passed to {@link DatagramSocket#send}
     */
    public void send(DatagramPacket packet) {
        try {
            super.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
