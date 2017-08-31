package ru.chemist.highloadcup;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                                      Mode  Cnt  Score   Error  Units
 RequestHandlerBenchmark.benchmarkAvg           avgt    5  3,821 ± 2,492  us/op
 RequestHandlerBenchmark.benchmarkGetAllVisits  avgt    5  6,228 ± 1,200  us/op
 RequestHandlerBenchmark.benchmarkGetLocation   avgt    5  1,128 ± 0,287  us/op
 RequestHandlerBenchmark.benchmarkGetUser       avgt    5  1,023 ± 0,109  us/op
 RequestHandlerBenchmark.benchmarkGetVisit      avgt    5  0,832 ± 0,181  us/op
 */
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 2)
@Fork(1)
@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 2)
//@Threads(2)
public class RequestHandlerBenchmark {
    private final Repository repository = new Repository();
    private final RequestHandler handler = new RequestHandler(repository);
    private final Request request = new Request();
    private final Response response = new Response();
    private int id;

    @Setup
    public void load() throws Exception {
        ZipLoader.loadZip(repository);
    }

    private Request prepare(Request request, String uri) {
        byte[] line = ("GET " + uri + " HTTP1/1").getBytes();
        System.arraycopy(line, 0, request.buf, 0, line.length);
        request.uriStart = 4;
        request.uriEnd = 4 + uri.length() - 1;
        FastUriParser.parseUri(request);
        return request;
    }

    @Benchmark
    public void benchmarkGetUser() throws Exception {
        try {
            id = (id + 1) % 100;
            request.method = HttpMethod.GET;
            prepare(request, "/users/" + (id + 1));
//            handler.handle(request, response);
            if (response.status != Status.OK) throw new RuntimeException("Invalid status for id " + id + ": " + response.status);
            if (response.contentLength == 0) throw new RuntimeException("Invalid response for id "+id+": " + Arrays.toString(response.content));
        } finally {
            request.reset();
            response.reset();
        }
    }

    @Benchmark
    public void benchmarkGetLocation() throws Exception {
        try {
            id = (id + 1) % 100;
            request.method = HttpMethod.GET;
            prepare(request, "/locations/" + (id + 1));
//            handler.handle(request, response);
            if (response.status != Status.OK) throw new RuntimeException("Invalid status for id " + id + ": " + response.status);
            if (response.contentLength == 0) throw new RuntimeException("Invalid response for id "+id+": " + Arrays.toString(response.content));
        } finally {
            request.reset();
            response.reset();
        }
    }

    @Benchmark
    public void benchmarkGetVisit() throws Exception {
        try {
            id = (id + 1) % 100;
            request.method = HttpMethod.GET;
            prepare(request, "/visits/" + (id + 1));
//            handler.handle(request, response);
            if (response.status != Status.OK) throw new RuntimeException("Invalid status for id " + id + ": " + response.status);
            if (response.contentLength == 0) throw new RuntimeException("Invalid response for id "+id+": " + Arrays.toString(response.content));
        } finally {
            request.reset();
            response.reset();
        }
    }

    @Benchmark
    public void benchmarkGetAllVisits() throws Exception {
        try {
            id = (id + 1) % 100;
            request.method = HttpMethod.GET;
            prepare(request, "/users/" + (id + 1) + "/visits?fromDate=0&toDate=1223268286&toDistance=99999");
//            handler.handle(request, response);
            if (response.status != Status.OK) throw new RuntimeException("Invalid status for id " + id + ": " + response.status);
            if (response.contentLength == 0) throw new RuntimeException("Invalid response for id "+id+": " + Arrays.toString(response.content));
//            System.out.println("|" +new String(response.content, 0, response.contentLength, StandardCharsets.UTF_8)+"|");
        } finally {
            request.reset();
            response.reset();
        }
    }

    @Benchmark
    public void benchmarkAvg() throws Exception {
        try {
            id = (id + 1) % 100;
            request.method = HttpMethod.GET;
            prepare(request, "/locations/" + (id + 1) + "/avg?fromDate=0&toDate=1223268286&fromAge=10&toAge=100&gender=f");
//            handler.handle(request, response);
            if (response.status != Status.OK) throw new RuntimeException("Invalid status for id " + id + ": " + response.status);
            if (response.contentLength == 0) throw new RuntimeException("Invalid response for id "+id+": " + Arrays.toString(response.content));
        } finally {
            request.reset();
            response.reset();
        }
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RequestHandlerBenchmark.class.getName())
//                .include("benchmarkGetUser")
                .build();
        new Runner(opt).run();
    }
}
