package ru.chemist.highloadcup;

public class CacheKey {
    public static final byte[] EMPTY = new byte[0];
    private byte[] buf;
    private int offset;
    private int size;
    private int hashCode;
    public CacheKey next;

    public void init(byte[] buf, int offset, int size) {
        this.buf = buf;
        this.offset = offset;
        this.size = size;
        this.hashCode = hashCode(buf, offset, size);
    }

    @Override
    public boolean equals(Object o) {
        CacheKey k2 = (CacheKey) o;
        return equals(buf, offset, size, k2.buf, k2.offset, k2.size);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private static boolean equals(byte[] a, int aOffset, int aSize, byte[] a2, int a2Offset, int a2Size) {
        if (a==a2)
            return true;
//        if (a==null || a2==null)
//            return false;

        if (a2Size != aSize)
            return false;

        for (int i = 0; i< aSize; i++)
            if (a[aOffset + i] != a2[a2Offset + i])
                return false;

        return true;
    }

    private static int hashCode(byte a[], int offset, int size) {
        int result = 1;
        for (int i = offset; i < offset + size; i++) {
            byte element = a[i];
            result = 31 * result + element;
        }

        return result;
    }

    public void makeImmutable(BytePool pool) {
        if (size == 0) {
            buf = EMPTY;
            offset = 0;
            return;
        }
        byte[] requestCopy = pool.get(size);
        JsonUtil.arraycopy(buf, offset, requestCopy, 0, size);
        buf = requestCopy;
        offset = 0;
    }
}
