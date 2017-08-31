package ru.chemist.highloadcup;

public class JsonUtil {
    public static void arraycopy(byte[] src, int pos, byte[] dst, int offset, int len) {
        if (len > 10)
            System.arraycopy(src, pos, dst, offset, len);
        else
            for(int i=pos;i<pos + len;i++) {
                dst[offset++] = src[i];
            }
    }
}
