package com.tstorm.tftp.packet;

import java.util.Optional;

/**
 * Placeholder for the packet so that all the packets can be constructed
 * at the beginnging of the protocol, but the costly process of actually
 * reading the bytes from a file is only performed when the packet is needed.
 */
public class ProxyPacket implements Packet {

    private Optional<RealPacket> packet = Optional.empty();
    private final int id, blockSize;

    public ProxyPacket(int id, int size) {
        this.id = id;
        this.blockSize = size;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] createPayload() {
        if (!packet.isPresent()) {
            this.packet = Optional.of(new RealPacket(this.id, this.blockSize));
        }
        return this.packet.get().createPayload();
    }

}
