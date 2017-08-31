package ru.chemist.highloadcup;

import java.nio.ByteBuffer;

public interface CacheHolder {
    void setEntityCache(ByteBuffer bb);

    void setQueryCache(CacheKey key, ByteBuffer bb);
}
