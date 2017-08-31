package ru.chemist.highloadcup;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                            Mode  Cnt  Score   Error  Units
 HttpUtilBenchmark.benchmarkTryParse  avgt    5  0,486 ± 0,120  us/op
 */
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 2)
@Fork(1)
@State(Scope.Benchmark)
@Measurement(iterations = 5, time = 2)
//@Threads(2)
public class HttpUtilBenchmark {
    private final Request request = new Request();
    private final Response response = new Response();

    @Setup
    public void init() throws Exception {
        byte[] req = ("POST /visits/2477?query_id=0 HTTP/1.1\r\n" +
                "Host: travels.com\r\n" +
                "User-Agent: tank\r\n" +
                "Accept: */*\r\n" +
                "Connection: Close\r\n" +
                "Content-Length: 11\r\n" +
                "Content-Type: application/json\r\n" +
                "\r\n" +
                "{\"mark\": 3}\r\n" +
                "\r\n").getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(req, 0, request.buf, 0, req.length);
        request.bufSize = req.length;
        byte[] EMPTY_RESPONSE = "{}".getBytes();
        response.content(EMPTY_RESPONSE);
    }

    @Benchmark
    public void benchmarkTryParse() throws Exception {
        try {
            HttpUtil.tryParse(request);
            if (request.method != HttpMethod.POST) throw new RuntimeException();
        } finally {
            request.method = -1;
        }
    }

//    @Benchmark
//    public void benchmarkEmptySend() throws Exception {
//        try {
//            HttpUtil.makeResponse(null, response, null buf -> {
//                if (buf.limit() <= 0) throw new RuntimeException();
//            });
//        } finally {
//            response.reset();
//        }
//    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(HttpUtilBenchmark.class.getName())
                .build();
        new Runner(opt).run();
    }
}
