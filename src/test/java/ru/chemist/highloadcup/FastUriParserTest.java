package ru.chemist.highloadcup;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class FastUriParserTest {

    private Request prepare(Request request, String uri) {
        byte[] line = ("GET " + uri + " HTTP1/1").getBytes();
        System.arraycopy(line, 0, request.buf, 0, line.length);
        request.uriStart = 4;
        request.uriEnd = 4 + uri.length() - 1;
        FastUriParser.parseUri(request);
        return request;
    }

    @Test
    public void parseGetValue() throws Exception {

        MutableObject<String> k = new MutableObject<>();
        Request request = new Request();


        FastUriParser.parse(prepare(request, "/aaaa?k=3"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "k".getBytes())) {
                k.setValue(FastUriParser.getValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is("3"));


        FastUriParser.parse(prepare(request, "/aaaa?k=3&d=5"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "d".getBytes())) {
                k.setValue(FastUriParser.getValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is("5"));


        FastUriParser.parse(prepare(request, "/aaaa?k=3&d="), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "d".getBytes())) {
                k.setValue(FastUriParser.getValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(""));


        FastUriParser.parse(prepare(request, "/aaaa?k=&d="), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "k".getBytes())) {
                k.setValue(FastUriParser.getValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(""));


        FastUriParser.parse(prepare(request, "/aaaa?k=&d=&f"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "f".getBytes())) {
                k.setValue(FastUriParser.getValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(""));

        k.setValue(null);
        FastUriParser.parse(prepare(request, "/aaaa?k=&d=&f"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "z".getBytes())) {
                k.setValue(FastUriParser.getValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(nullValue()));
    }

    @Test
    public void parseGetStringValue() throws Exception {

        MutableObject<String> k = new MutableObject<>();
        Request request = new Request();

        FastUriParser.parse(prepare(request, "/aaaa?k=3"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "k".getBytes())) {
                k.setValue(FastUriParser.getStringValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is("3"));


        FastUriParser.parse(prepare(request, "/aaaa?k=3&d=5"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "d".getBytes())) {
                k.setValue(FastUriParser.getStringValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is("5"));


        FastUriParser.parse(prepare(request, "/aaaa?k=3&d="), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "d".getBytes())) {
                k.setValue(FastUriParser.getStringValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(""));


        FastUriParser.parse(prepare(request, "/aaaa?k=&d="), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "k".getBytes())) {
                k.setValue(FastUriParser.getStringValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(""));


        FastUriParser.parse(prepare(request, "/aaaa?k=&d=&f"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "f".getBytes())) {
                k.setValue(FastUriParser.getStringValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(""));

        k.setValue(null);
        FastUriParser.parse(prepare(request, "/aaaa?k=&d=&f"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "z".getBytes())) {
                k.setValue(FastUriParser.getStringValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(nullValue()));


        FastUriParser.parse(prepare(request, "/aaaa?k=&d=%20&f"), (uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, "d".getBytes())) {
                k.setValue(FastUriParser.getStringValue(uri, eq, end));
            }
        });

        assertThat(k.getValue(), is(" "));
    }

}