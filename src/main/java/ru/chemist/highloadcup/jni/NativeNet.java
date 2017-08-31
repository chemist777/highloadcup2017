package ru.chemist.highloadcup.jni;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class NativeNet {
    static
    {
        try {
            try (InputStream in = NativeNet.class.getResourceAsStream("/nativenet")) {
                Path path = Files.createTempFile("nativenet", null);
                byte[] data = new byte[in.available()];
                in.read(data);
                Files.write(path, data);
                System.load(path.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public native void test();
    public native long bind(int port);
    public native void unbind(long fd);
    public native void close(long fd);
    public native int read(long fd, long bbAddress, int size, int offset);
    public native int write(long fd, ByteBuffer buf, int size);
    public static native void init();
    public static native long epollCreate();
    public static native int getEvents(long epollId, long serverSocketId, long bbAddress, long eventsBuffer);
    public static native int epollListen(long epollId, long fd);
    public static native long eventsBuffer();
}
