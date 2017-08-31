package ru.chemist.highloadcup;

import java.io.IOException;
import java.io.OutputStream;

public class FastOutputStream extends OutputStream {
    private final Response response;

    public FastOutputStream(Response response) {
        this.response = response;
    }

    @Override
    public void write(int b) throws IOException {
        response.content[response.contentLength++] = (byte) b;
    }

    @Override
    public void write(byte[] b) throws IOException {
        JsonUtil.arraycopy(b, 0, response.content, response.contentLength, b.length);
        response.contentLength += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        JsonUtil.arraycopy(b, off, response.content, response.contentLength, len);
        response.contentLength += len;
    }
}
