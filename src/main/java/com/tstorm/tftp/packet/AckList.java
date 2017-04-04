package com.tstorm.tftp.packet;

import java.util.BitSet;

/**
 * Data structure to buffer ACKs that occured out of order
 */
public class AckList extends BitSet {

    /**
     * The window size won't change and must be a power of two
     * so we can use a bit AND to perform modulus.
     */
    private final int windowSize;

    public AckList(int windowSize) {
        super(windowSize);
        this.windowSize = windowSize;
    }

    /**
     * Flips a bit in the list to record that ack was received
     *
     * @param seqNum The sequence number of the packet to buffer
     */
    public void bufferAck(int seqNum) {
        set(seqNum & (windowSize - 1));
    }

    /**
     * Gives the total consecutive buffered ACKs from the given index
     *
     * @param index The sequence number of the packet from which to start
     *              the search. This index should be the first potentially
     *              buffered packet.
     * @return The next unACKed packet from the given index. If there are
     *  no buffered ACKs then the return value is index + 1. If there are
     *  n buffered ACKs, then the return value is index + n + 1.
     */
    public int getBufferedAckCount(int index) {
        int bufferedAckSum = 1;
        while (get(index & (windowSize - 1))) {
            index += 1;
            if (++bufferedAckSum == windowSize) {
                break;
            }
        }
        if (bufferedAckSum > 1) {
            int flushStart = index & (windowSize - 1);
            int flushEnd = flushStart + bufferedAckSum;
            clear(flushStart, flushEnd);
        }
        return bufferedAckSum;
    }

}
