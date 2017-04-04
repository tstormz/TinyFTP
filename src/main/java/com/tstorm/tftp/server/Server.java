package com.tstorm.tftp.server;

import java.io.FileNotFoundException;
import java.net.SocketException;

public class Server {

    public static void main(String[] args) {
        try {
            new Thread(new Receiver()).start();
        } catch (SocketException | FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
