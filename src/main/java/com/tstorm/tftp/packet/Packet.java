package com.tstorm.tftp.packet;

public interface Packet {

    /**
     * Reads the actual data from the file being uploaded.
     *
     * @return The payload as a sequence of bytes
     */
    byte[] createPayload();

}
