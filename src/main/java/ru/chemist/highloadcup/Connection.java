package ru.chemist.highloadcup;

public class Connection {
    public final Request request = new Request();
    public final Response response = new Response();
//    public long lastActivityMs;
    public Connection() {
//        lastActivityMs = System.currentTimeMillis();
    }

    public void reset() {
        request.reset();
        response.reset();
    }
}
