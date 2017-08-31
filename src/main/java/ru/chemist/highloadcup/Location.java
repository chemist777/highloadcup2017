package ru.chemist.highloadcup;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Location implements CacheHolder {
    /*
    id - уникальный внешний id достопримечательности. Устанавливается тестирующей системой. 32-разрядное целое число.
place - описание достопримечательности. Текстовое поле неограниченной длины.
country - название страны расположения. unicode-строка длиной до 50 символов.
city - название города расположения. unicode-строка длиной до 50 символов.
distance - расстояние от города по прямой в километрах. 32-разрядное целое число.
     */
    public final int id;
    public String place;
    public String country;
    public String city;
    public long distance;

    public List<Visit> visitsCache;

    public volatile ByteBuffer bEntityCache;
    public volatile Map<CacheKey, ByteBuffer> bAvgCache;

    public Location(int id, String place, String country, String city, long distance) {
        this.id = id;
        this.place = place;
        this.country = country;
        this.city = city;
        this.distance = distance;
    }

    @Override
    public void setEntityCache(ByteBuffer bb) {
        bEntityCache = bb;
    }

    @Override
    public void setQueryCache(CacheKey key, ByteBuffer bb) {
        if (bAvgCache == null) bAvgCache = new HashMap<>(1, 1.00f);
        bAvgCache.put(key, bb);
    }

    public void clearQueryCache() {
        if (bAvgCache != null) bAvgCache.clear();
    }

    public ByteBuffer get(CacheKey key) {
        return bAvgCache == null ? null : bAvgCache.get(key);
    }

    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", place='" + place + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", distance=" + distance +
                '}';
    }
}
