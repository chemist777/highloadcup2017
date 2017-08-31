package ru.chemist.highloadcup;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class BytesUtil {
    static Field field;
    static {
        try {
            field = Buffer.class.getDeclaredField("address");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
    }

    public static long getAddress(ByteBuffer buffer) {
        try {
            return field.getLong(buffer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
