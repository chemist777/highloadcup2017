package ru.chemist.highloadcup;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Request {
    public final ByteBuffer directBuf = ByteBuffer.allocateDirect(32 * 1024);
    public final long directBufAddress = BytesUtil.getAddress(directBuf);
    //original request buffer
    public final byte[] buf = new byte[32 * 1024];
    public int bufSize;

    public int contentOffset;
    public int contentLen;

    public int uriStart;
    public int uriEnd;

    public int pathStart;
    public int pathEnd;

    public int method;
    public boolean keepalive;

    public int entity;
    public int id;
    public int action;

    public int expectedRequestSize;

    public Request() {
        reset();
    }

    public void reset() {
        uriStart = 0;
        uriEnd = 0;
        pathStart = 0;
        pathEnd = 0;
        
        method = -1;
        contentOffset = -1;
        contentLen = 0;
        bufSize = 0;
        keepalive = true;
        directBuf.clear();

        entity = Entity.NOT_FOUND;
        id = -1;
        action = Action.NOT_FOUND;

        expectedRequestSize = -1;
    }

    public String substring(int startPos, int endPosExclusive) {
        return new String(buf, startPos, endPosExclusive - startPos, StandardCharsets.ISO_8859_1);
    }
}
