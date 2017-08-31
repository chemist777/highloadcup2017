package ru.chemist.highloadcup;

import java.nio.ByteBuffer;

public interface ByteBufferPool {
    int BUCKETS = 22;

    ByteBuffer get(int size);

    void clear();

    long totalBytesAllocated();

    int overflow(int index);
}
