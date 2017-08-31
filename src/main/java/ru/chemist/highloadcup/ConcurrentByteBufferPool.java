package ru.chemist.highloadcup;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static ru.chemist.highloadcup.MathUtil.roundUpPowerOfTwo;

public class ConcurrentByteBufferPool implements ByteBufferPool {
    public static final int BUCKETS = 22;
    private ByteBuffer[][] pool;
    private AtomicIntegerArray nextIndexes;
    public AtomicIntegerArray overflow;
    private long bytes;
    public long totalBytesAllocated;
    public static int prealloc[] = new int[BUCKETS];

    static {
        //prod 18x
        prealloc[0] = 1;
        prealloc[1] = 2;
        prealloc[2] = 2;
        prealloc[3] = 2;
        prealloc[4] = 2;
        prealloc[5] = 2;
        prealloc[6] = 2;
        prealloc[7] = 46000;
        prealloc[8] = 73000;
        prealloc[9] = 4000;
        prealloc[10] = 3000;
        prealloc[11] = 1600;
        prealloc[12] = 330;
        prealloc[13] = 35;
        prealloc[14] = 1;
        prealloc[15] = 1;
        prealloc[16] = 1;
        prealloc[17] = 1;
        prealloc[18] = 0;
        prealloc[19] = 0;
        prealloc[20] = 0;
    }

    public ConcurrentByteBufferPool() {
        //make buckets for buffers with size 2^21. First element is 2^0
        pool = new ByteBuffer[prealloc.length][];
        overflow = new AtomicIntegerArray(prealloc.length);
        nextIndexes = new AtomicIntegerArray(prealloc.length);

        for(int i=0;i<prealloc.length;i++) {
            preallocate(i, prealloc[i]);
        }

        totalBytesAllocated = bytes;

        System.out.println("Preallocated " + bytes + " bytes of direct memory");
    }

    private void preallocate(int bufferSizeIndex, int count) {
        ByteBuffer[] singleSizePool = pool[bufferSizeIndex] = new ByteBuffer[count];
        int size = 1 << bufferSizeIndex;
        bytes += size * count;
        for(int i=0;i<count;i++) {
            singleSizePool[i] = ByteBuffer.allocateDirect(size);
        }
    }

    @Override
    public ByteBuffer get(int size) {
        assert size > 0;
        int bufferSizeIndex = Integer.numberOfTrailingZeros(roundUpPowerOfTwo(size));
        assert bufferSizeIndex < pool.length;
        ByteBuffer[] singleSizePool = pool[bufferSizeIndex];
        int nextIndex;
        if ((nextIndex = nextIndexes.getAndIncrement(bufferSizeIndex)) < singleSizePool.length) {
            ByteBuffer bb = singleSizePool[nextIndex];
            bb.clear();
            return bb;
        } else {
            //allocate new one
            overflow.getAndIncrement(bufferSizeIndex);
            totalBytesAllocated += size;
//            if (showErrors) System.out.println("No free buffer with size " + bufferSizeIndex + ". Want " + totalThreads * (singleSizePool.length + over) + " buffers.");
            return ByteBuffer.allocateDirect(size);
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
