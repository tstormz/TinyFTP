package packet;

import com.tstorm.tftp.packet.AckList;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AckListTest {

    private static final int WINDOW_SIZE = 8;
    private AckList ackList;

    @Before
    public void initialize() {
        ackList = new AckList(WINDOW_SIZE);
    }

    @Test
    public void testBufferAckSimple() {
        ackList.bufferAck(2);
        ackList.bufferAck(3);
        assertEquals(false, ackList.get(1));
        assertEquals(true, ackList.get(2));
        assertEquals(true, ackList.get(3));
        assertEquals(false, ackList.get(4));
    }

    @Test
    public void testBufferAckWraparound() {
        ackList.bufferAck(WINDOW_SIZE - 1);
        ackList.bufferAck(WINDOW_SIZE);
        ackList.bufferAck(WINDOW_SIZE + 1);
        assertEquals(false, ackList.get(6));
        assertEquals(true, ackList.get(7));
        assertEquals(true, ackList.get(0));
        assertEquals(true, ackList.get(1));
        assertEquals(false, ackList.get(2));
    }

    @Test
    public void testBufferedAckCount() {
        // assume LAR is 15
        int lastAckReceived = 15;
        // if we are checking the ack list, then ack #10 has arrived and
        // we want to see if 11 was was buffered
        int firstPotentialBufferedAck = lastAckReceived + 2;
        assertEquals(1, ackList.getBufferedAckCount(firstPotentialBufferedAck));
        // try again with 2 buffered acks
        ackList.bufferAck(firstPotentialBufferedAck);
        ackList.bufferAck(firstPotentialBufferedAck + 1);
        // add one with a gap
        ackList.bufferAck(firstPotentialBufferedAck + 3);
        assertEquals(3, ackList.getBufferedAckCount(firstPotentialBufferedAck));
    }

}
