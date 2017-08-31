package ru.chemist.highloadcup;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements CacheHolder {
    /*
    id - уникальный внешний идентификатор пользователя. Устанавливается тестирующей системой и используется затем, для проверки ответов сервера. 32-разрядное целое число.
email - адрес электронной почты пользователя. Тип - unicode-строка длиной до 100 символов. Гарантируется уникальность.
first_name и last_name - имя и фамилия соответственно. Тип - unicode-строки длиной до 50 символов.
gender - unicode-строка "m" означает мужской пол, а "f" - женский.
birth_date - дата рождения, записанная как число секунд от начала UNIX-эпохи по UTC (другими словами - это timestamp). Ограничено снизу 01.01.1930 и сверху 01.01.1999-ым.
     */
    public final int id;
    public String email;
    public String firstName;
    public String lastName;
    public byte gender;
    public long birthDate;

    public List<Visit> visitsCache;

    public volatile ByteBuffer bEntityCache;
    public volatile Map<CacheKey, ByteBuffer> bVisitsCache;

    public User(int id, String email, String firstName, String lastName, byte gender, long birthDate) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    @Override
    public void setEntityCache(ByteBuffer bb) {
        bEntityCache = bb;
    }

    @Override
    public void setQueryCache(CacheKey key, ByteBuffer bb) {
        if (bVisitsCache == null) bVisitsCache = new HashMap<>(1, 1.00f);
        bVisitsCache.put(key, bb);
    }

    public void clearQueryCache() {
        if (bVisitsCache != null) bVisitsCache.clear();
    }

    public ByteBuffer get(CacheKey key) {
        return bVisitsCache == null ? null : bVisitsCache.get(key);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", gender='" + gender + '\'' +
                ", birthDate=" + birthDate +
                '}';
    }
}
