package ru.chemist.highloadcup;

import java.nio.ByteBuffer;

public class Visit implements Comparable<Visit>, CacheHolder {
    /*
    id - уникальный внешний id посещения. Устанавливается тестирующей системой. 32-разрядное целое число.
location - id достопримечательности. 32-разрядное целое число.
user - id путешественника. 32-разрядное целое число.
visited_at - дата посещения, timestamp с ограничениями: снизу 01.01.2000, а сверху 01.01.2015.
mark - оценка посещения от 0 до 5 включительно. Целое число.
     */
    public final int id;
    public int location;
    public int user;
    public long visitedAt;
    public byte mark;

    public User userRef;

    public Location locationRef;

    public volatile ByteBuffer bEntityCache;

    public Visit(int id, int location, int user, long visitedAt, int mark) {
        this.id = id;
        this.location = location;
        this.user = user;
        this.visitedAt = visitedAt;
        this.mark = (byte) mark;
    }

    @Override
    public int compareTo(Visit v) {
        long r = visitedAt - v.visitedAt;
        if (r < 0)
            return -1;
        else if (r > 0)
            return 1;
        else {
            r = id - v.id;
            if (r < 0)
                return -1;
            else if (r > 0)
                return 1;
            else
                return 0;
        }
    }

    @Override
    public void setEntityCache(ByteBuffer bb) {
        bEntityCache = bb;
    }

    @Override
    public void setQueryCache(CacheKey key, ByteBuffer bb) {
        //
    }


    @Override
    public String toString() {
        return "Visit{" +
                "id=" + id +
                ", location=" + location +
                ", user=" + user +
                ", visitedAt=" + visitedAt +
                ", mark=" + mark +
                '}';
    }
}
