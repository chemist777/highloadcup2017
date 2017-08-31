package ru.chemist.highloadcup;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.chemist.highloadcup.warmup.Client;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark                           Mode  Cnt    Score     Error  Units
 TestBenchmark.benchmarkEmpty        avgt    5  126,551 ±  95,119  us/op
 TestBenchmark.benchmarkLocationAvg  avgt    5   41,225 ± 19,717  us/op
 TestBenchmark.benchmarkUserGet      avgt    5   45,984 ± 24,651  us/op
 TestBenchmark.benchmarkUserPost     avgt    5  121,324 ± 46,894  us/op
 TestBenchmark.benchmarkUserVisits   avgt    5   58,472 ± 32,689  us/op
 TestBenchmark.benchmarkVisitPost    avgt    5  122,801 ± 58,610  us/op
 */
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 2)
@Fork(1)
@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 2)
//@Threads(2)
public class TestBenchmark {
    private final Client client = new Client();
    private int id;

//    @Benchmark
//    public void benchmarkEmpty() throws Exception {
//        id = (id + 1) % 100;
//        String data = client.get("/1");
//        if (!data.startsWith("{")) throw new RuntimeException("Bad response: " + data);
//    }

    @Benchmark
    public void benchmarkUserPost() throws Exception {
        id = (id + 1) % 100;
        String data = client.post("/users/" + (id+1), "{\"first_name\":\"кака\"}");
        if (!data.startsWith("{")) throw new RuntimeException("Bad response: " + data);
    }

    @Benchmark
    public void benchmarkVisitPost() throws Exception {
        id = (id + 1) % 100;
        String data = client.post("/visits/" + (id+1), "{\"user\":"+(id+1)+",\"location\":"+(id+1)+",\"visited_at\":"+(100000+id)+"}");
        if (!data.startsWith("{")) throw new RuntimeException("Bad response: " + data);
    }

    @Benchmark
    public void benchmarkUserGet() throws Exception {
        id = (id + 1) % 100;
        String data = client.get("/users/"+(id+1));
        if (!data.startsWith("{")) throw new RuntimeException("Bad response: " + data);
    }

    @Benchmark
    public void benchmarkUserVisits() throws Exception {
        id = (id + 1) % 100;
        String data = client.get("/users/"+(id+1)+"/visits");
        if (!data.startsWith("{")) throw new RuntimeException("Bad response: " + data);
    }

    @Benchmark
    public void benchmarkLocationAvg() throws Exception {
        id = (id + 1) % 100;
        String data = client.get("/locations/"+(id+1)+"/avg");
        if (!data.startsWith("{")) throw new RuntimeException("Bad response: " + data);
    }


    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(TestBenchmark.class.getSimpleName())
//                .include("TestBenchmark.benchmarkEmpty")
                .build();
        new Runner(opt).run();
    }
}
