package ru.chemist.highloadcup;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * from 10 bytes system.arraycopy is faster
 */
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 1)
@Fork(0)
@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 2)
//@Threads(2)
public class JsonUtilBenchmark {
//    @Param({"6", "30", "100", "256"})
    @Param({"7", "8", "9"})
    public int size;

    private final byte[] src = new byte[6 * 1024];
    private final byte[] dst = new byte[6 * 1024];

    @Setup
    public void init() throws Exception {
        new Random().nextBytes(src);
    }

    private void copy(byte[] src, int pos, byte[] dst, int offset, int len) {
        for(int i=pos;i<pos + len;i++) {
            dst[offset++] = src[i];
        }
    }

    @Benchmark
    public void benchmarkJavaArrayCopy() throws Exception {
        copy(src, 0, dst, 0, size);
    }

    @Benchmark
    public void benchmarkSystemArrayCopy() throws Exception {
        System.arraycopy(src, 0, dst, 0, size);
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(JsonUtilBenchmark.class.getName())
                .build();
        new Runner(opt).run();
    }
}
