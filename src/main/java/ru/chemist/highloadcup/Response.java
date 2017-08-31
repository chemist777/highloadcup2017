package ru.chemist.highloadcup;

import java.nio.ByteBuffer;

public class Response {
    public final byte[] buf = new byte[16 * 1024];
    public Status status;
    public final byte[] contentBuffer = new byte[16 * 1024];
    public byte[] content;
    public int contentLength;
    public Runnable afterResponseSent;

    public final FastOutputStream outputStream = new FastOutputStream(this);

    public ByteBuffer predefinedResponse;

    public Response() {
        reset();
    }

    public void reset() {
        status = null;
        contentLength = 0;
        content = contentBuffer;
        afterResponseSent = null;
        predefinedResponse = null;
    }

    public void content(byte[] constantBytes) {
        content = constantBytes;
        contentLength = constantBytes.length;
    }
}
