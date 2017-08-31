package ru.chemist.highloadcup;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                                      Mode  Cnt    Score    Error  Units
 MapAllocateVsClearBenchmark.benchmarkAllocate  avgt    5    0,119 ±  0,037  us/op
 MapAllocateVsClearBenchmark.benchmarkClear     avgt    5  762,376 ± 34,448  us/op
 */
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 2)
@Fork(1)
@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 2)
@Threads(4)
public class MapAllocateVsClearBenchmark {
    private volatile Map<CacheKey, ByteBuffer> map;
    private CacheKey[] keys;
    private ByteBuffer value;
    private static final int keyCount = 150000;
    private Random random;

    @Setup
    public void init() throws Exception {
        map = new ConcurrentHashMap<>(1024, 0.75f, 4);
        value = ByteBuffer.allocate(128);
        random = new Random();
        keys = new CacheKey[keyCount];
        for(int i=0;i<keyCount;i++) {
            keys[i] = new CacheKey();
            byte[] key = new byte[1024];
            random.nextBytes(key);
            keys[i].init(key, 0, key.length);
        }
        fill();
    }

    @Benchmark
    public void benchmarkClear() throws Exception {
        map.clear();
    }

    @Benchmark
    public void benchmarkAllocate() throws Exception {
        map = new ConcurrentHashMap<>(1024, 0.75f, 4);
    }

    private void fill() {
        for (CacheKey key : keys) {
            map.put(key, value);
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(MapAllocateVsClearBenchmark.class.getName())
                .build();
        new Runner(opt).run();
    }
}
