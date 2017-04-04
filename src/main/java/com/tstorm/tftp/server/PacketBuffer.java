package com.tstorm.tftp.server;

public class PacketBuffer<T> {

    private final int modulus;
    private volatile Object[] buffer;
    private int head, tail;

    public PacketBuffer(int capacity) {
        this.buffer = new Object[capacity];
        this.modulus = capacity - 1;
    }

    public void add(T packet) {
        buffer[tail++ & modulus] = packet;
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        return (T) buffer[head++ & modulus];
    }

    public boolean isFull() {
        return (tail & modulus) == 0;
    }

    public boolean isEmpty() {
        return head == tail;
    }

}
