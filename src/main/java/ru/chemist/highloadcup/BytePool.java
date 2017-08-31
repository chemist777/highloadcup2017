package ru.chemist.highloadcup;

public interface BytePool {
    int BUCKETS = 16;

    byte[] get(int size);

    void clear();

    long totalBytesAllocated();

    int overflow(int index);
}
