package ru.chemist.highloadcup;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 2)
@Fork(1)
@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 2)
//@Threads(2)
public class NumberFormatBenchmark {
    Random random = new Random();
    DecimalFormat df = new DecimalFormat("0.#####");

    public NumberFormatBenchmark() {
        df.setRoundingMode(RoundingMode.HALF_UP);
    }

    @Benchmark
    public void df() {
        double avg = random.nextDouble();
        String r = df.format(avg);
        if (r.length() == 0) throw new RuntimeException();
    }

    @Benchmark
    public void nativeFormat() {
        double avg = random.nextDouble();
        avg = (double)Math.round(avg * 100000d) / 100000d;
        String r = String.valueOf(avg);
        if (r.length() == 0) throw new RuntimeException();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(NumberFormatBenchmark.class.getName())
                .build();
        new Runner(opt).run();
    }
}
