package ru.chemist.highloadcup;


import java.util.concurrent.atomic.AtomicIntegerArray;

import static ru.chemist.highloadcup.MathUtil.roundUpPowerOfTwo;

public class ConcurrentBytePool implements BytePool {
    public static final int BUCKETS = 16;
    private byte[][][] pool;
    private AtomicIntegerArray nextIndexes;
    public AtomicIntegerArray overflow;
//    private final int totalThreads;
    private long bytes;
    public long totalBytesAllocated;
    public static int prealloc[] = new int[BUCKETS];

    static {
        prealloc[0] = 0;
        prealloc[1] = 0;
        prealloc[2] = 0;
        prealloc[3] = 0;
        prealloc[4] = 7000;
        prealloc[5] = 18000;
        prealloc[6] = 17000;
        prealloc[7] = 1688 * 18;
        prealloc[8] = 89406;
        prealloc[9] = 810;
        prealloc[10] = 2;
        prealloc[11] = 2;
        prealloc[12] = 2;
        prealloc[13] = 1;
        prealloc[14] = 1;
        prealloc[15] = 1;
    }
//    public volatile boolean showErrors;

    public ConcurrentBytePool() {
//        this.totalThreads = totalThreads;


        //make buckets for buffers with size 2^21. First element is 2^0
        pool = new byte[prealloc.length][][];
        overflow = new AtomicIntegerArray(prealloc.length);
        nextIndexes = new AtomicIntegerArray(prealloc.length);

        for(int i=0;i<prealloc.length;i++) {
            preallocate(i, prealloc[i]);
        }

        totalBytesAllocated = bytes;

        System.out.println("Preallocated " + bytes + " bytes of byte[] memory");
    }

    private void preallocate(int bufferSizeIndex, int count) {
        byte[][] singleSizePool = pool[bufferSizeIndex] = new byte[count][];
        int size = 1 << bufferSizeIndex;
        bytes += size * count;
        for(int i=0;i<count;i++) {
            singleSizePool[i] = new byte[size];
        }
    }

    @Override
    public byte[] get(int size) {
        assert size > 0;
        int bufferSizeIndex = Integer.numberOfTrailingZeros(roundUpPowerOfTwo(size));
        assert bufferSizeIndex < pool.length;
        byte[][] singleSizePool = pool[bufferSizeIndex];
        int nextIndex;
        if ((nextIndex = nextIndexes.getAndIncrement(bufferSizeIndex)) < singleSizePool.length) {
            return singleSizePool[nextIndex];
        } else {
            //allocate new one
            overflow.getAndIncrement(bufferSizeIndex);
            totalBytesAllocated += size;
//            if (showErrors) System.out.println("No free buffer with size " + bufferSizeIndex + ". Want " + totalThreads * (singleSizePool.length + over) + " buffers.");
            return new byte[size];
        }
    }

    @Override
    public void clear() {
        nextIndexes = new AtomicIntegerArray(nextIndexes.length());
        overflow = new AtomicIntegerArray(overflow.length());
        totalBytesAllocated = bytes;
    }

    @Override
    public long totalBytesAllocated() {
        return totalBytesAllocated;
    }

    @Override
    public int overflow(int index) {
        return overflow.get(index);
    }
}
