package server;

import com.tstorm.tftp.server.PacketBuffer;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class PacketBufferTest {

    private final int WINDOW_SIZE = 8;
    private PacketBuffer<Integer> buffer;

    @Before
    public void initialize() {
        buffer = new PacketBuffer<>(WINDOW_SIZE);
    }

    @Test
    public void testIsFull() {
        // try twice to test wrap around
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < WINDOW_SIZE - 1; j++) {
                buffer.add(j);
            }
            assertEquals(false, buffer.isFull());
            buffer.add(WINDOW_SIZE - 1);
            assertEquals(true, buffer.isFull());
        }
    }

    @Test
    public void testIsEmptySimple() {
        buffer.add(0);
        buffer.poll();
        assertEquals(true, buffer.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        buffer.add(0);
        buffer.poll();
        // fill the buffer
        for (int i = 0; i < WINDOW_SIZE; i++) {
            buffer.add(i);
        }
        assertEquals(false, buffer.isEmpty());
        // add one to ensure wrap around doesn't produce a false positive
        buffer.add(0);
        assertEquals(false, buffer.isEmpty());
        // flush buffer
        for (int i = 0; i < WINDOW_SIZE + 1; i++) {
            buffer.poll();
        }
        assertEquals(true, buffer.isEmpty());
    }

}
