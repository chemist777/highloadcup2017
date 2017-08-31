package ru.chemist.highloadcup;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static final byte[] EMPTY_RESPONSE = "{}".getBytes(StandardCharsets.ISO_8859_1);

    private final Repository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonFactory jsonFactory  = objectMapper.getFactory();

    private final ZonedDateTime now;

    //Ограничено снизу 01.01.1930 и сверху 01.01.1999-ым.
//    public static final long startBirth = LocalDate.of(1930, 1, 1).atStartOfDay(zoneId).toInstant().getEpochSecond();
//    public static final long endBirth = LocalDate.of(1999, 1, 2).atStartOfDay(zoneId).toInstant().getEpochSecond();
    //снизу 01.01.2000, а сверху 01.01.2015.
//    public static final long startVisit = LocalDate.of(2000, 1, 1).atStartOfDay(zoneId).toInstant().getEpochSecond();
//    public static final long endVisit = LocalDate.of(2015, 1, 2).atStartOfDay(zoneId).toInstant().getEpochSecond();

    public final ByteBuffer s200, s200Close, s404, s404Close, s400, s400Close;
    private static final byte[] fromDateName = "fromDate".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] toDateName = "toDate".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] countryName = "country".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] toDistanceName = "toDistance".getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] fromAgeName = "fromAge".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] toAgeName = "toAge".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] genderName = "gender".getBytes(StandardCharsets.ISO_8859_1);

    private String countryValue;
    private Long fromDate;
    private Long toDate;
    private Long toDistance;
    private Long fromAge;
    private Long toAge;
    byte genderByte;

    private int count, totalMark;

    private final SerializedString ID_FIELD = new SerializedString("id");
    private final SerializedString EMAIL_FIELD = new SerializedString("email");
    private final SerializedString FIRST_NAME_FIELD = new SerializedString("first_name");
    private final SerializedString LAST_NAME_FIELD = new SerializedString("last_name");
    private final SerializedString GENDER_FIELD = new SerializedString("gender");
    private final SerializedString BIRTH_DATE_FIELD = new SerializedString("birth_date");
    private final SerializedString M_VALUE = new SerializedString("m");
    private final SerializedString F_VALUE = new SerializedString("f");

    private final SerializedString MARK_FIELD = new SerializedString("mark");
    private final SerializedString VISITED_AT_FIELD = new SerializedString("visited_at");
    private final SerializedString PLACE_FIELD = new SerializedString("place");
    private final SerializedString USER_FIELD = new SerializedString("user");
    private final SerializedString LOCATION_FIELD = new SerializedString("location");
    private final SerializedString DISTANCE_FIELD = new SerializedString("distance");
    private final SerializedString CITY_FIELD = new SerializedString("city");
    private final SerializedString COUNTRY_FIELD = new SerializedString("country");
    private final SerializedString VISITS_FIELD = new SerializedString("visits");
    private final SerializedString AVG_FIELD = new SerializedString("avg");

    public RequestHandler(Repository repository) {
        this.repository = repository;
        now = ZonedDateTime.ofInstant(Instant.ofEpochSecond(repository.now), ZoneId.of("UTC"));
        s200 = makeResponse(Status.OK, EMPTY_RESPONSE);
        s200Close = makeResponse(Status.OK_NOKEEP, EMPTY_RESPONSE);
        s400 = makeResponse(Status.BAD_REQUEST, new byte[0]);
        s404 = makeResponse(Status.NOT_FOUND, new byte[0]);
        s400Close = makeResponse(Status.BAD_REQUEST_NOKEEP, new byte[0]);
        s404Close = makeResponse(Status.NOT_FOUND_NOKEEP, new byte[0]);
    }

    private static ByteBuffer makeResponse(Status status, byte[] content) {
        byte[] out = new byte[1024];
        int len = HttpUtil.makeResponse(status, content, content.length, out);
        ByteBuffer bb = ByteBuffer.allocateDirect(len);
        bb.put(out, 0, len);
        return bb;
    }

    public void handleGetUser(User user, Request request, Response response) throws Exception {

        try (JsonGenerator gen = jsonFactory.createGenerator(response.outputStream)) {
            gen.writeStartObject();

            gen.writeFieldName(ID_FIELD);
            gen.writeNumber(user.id);

            gen.writeFieldName(EMAIL_FIELD);
            gen.writeString(user.email);

            gen.writeFieldName(FIRST_NAME_FIELD);
            gen.writeString(user.firstName);

            gen.writeFieldName(LAST_NAME_FIELD);
            gen.writeString(user.lastName);

            gen.writeFieldName(GENDER_FIELD);
            gen.writeString(user.gender == 0 ? M_VALUE : F_VALUE);

            gen.writeFieldName(BIRTH_DATE_FIELD);
            gen.writeNumber(user.birthDate);

            gen.writeEndObject();
        }
    }

    public void handleGetUserVisits(User user, Request request, Response response) throws Exception {
/*
                    fromDate - посещения с visited_at > fromDate
toDate - посещения с visited_at < toDate
country - название страны, в которой находятся интересующие достопримечательности
toDistance - возвращать только те места, у которых расстояние от города меньше этого параметра

                     */
        fromDate = null;
        toDate = null;
        countryValue = null;
        toDistance = null;

        FastUriParser.parse(request, ((uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, fromDateName)) {
                int offset = eq + 1;
                int len = end - eq;
                if (!HttpUtil.isSignedNumeric(uri, offset, len)) {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                } else {
                    fromDate = HttpUtil.parseSignedLong(uri, offset, len);
                }
            } else if (FastUriParser.isParam(uri, start, toDateName)) {
                int offset = eq + 1;
                int len = end - eq;
                if (!HttpUtil.isSignedNumeric(uri, offset, len)) {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                } else {
                    toDate = HttpUtil.parseSignedLong(uri, offset, len);
                }
            } else if (FastUriParser.isParam(uri, start, countryName)) {
                countryValue = FastUriParser.getStringValue(uri, eq, end);
            } else if (FastUriParser.isParam(uri, start, toDistanceName)) {
                int offset = eq + 1;
                int len = end - eq;
                if (!HttpUtil.isNumeric(uri, offset, len)) {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                } else {
                    toDistance = HttpUtil.parseLong(uri, offset, len);
                }
            }
        }));

        if (response.predefinedResponse != null) {
            return;
        }

        List<Visit> list = user.visitsCache;
        if (list == null) list = Collections.emptyList();

                    /*
                    {
    "visits": [
        {
            "mark": 2,
            "visited_at": 1223268286,
            "place": "Кольский полуостров"
        },
        {
            "mark": 4,
            "visited_at": 958656902,
            "place": "Московский Кремль"
        },
        ...
     ]
}
                     */

        try (JsonGenerator gen = jsonFactory.createGenerator(response.outputStream)) {
            gen.writeStartObject();

            gen.writeFieldName(VISITS_FIELD);
            gen.writeStartArray();

            list.forEach(visit -> {
                if (fromDate != null) {
                    if (visit.visitedAt <= fromDate) return;
                }
                if (toDate != null) {
                    if (visit.visitedAt >= toDate) return;
                }
                if (countryValue != null) {
                    if (!visit.locationRef.country.equals(countryValue)) return;
                }
                if (toDistance != null) {
                    if (visit.locationRef.distance >= toDistance) return;
                }

                try {
                    gen.writeStartObject();

                    gen.writeFieldName(MARK_FIELD);
                    gen.writeNumber(visit.mark);

                    gen.writeFieldName(VISITED_AT_FIELD);
                    gen.writeNumber(visit.visitedAt);

                    gen.writeFieldName(PLACE_FIELD);
                    gen.writeString(visit.locationRef.place);

                    gen.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            gen.writeEndArray();
            gen.writeEndObject();
        }
    }

    public void handleNewUser(Request request, Response response) throws Exception {
        if (request.contentLen <= 0) {
            response.predefinedResponse = s400Close;
            return;
        }

        String email = null, firstName = null, lastName = null, gender = null;
        Long birthDate = null;
        Integer id = null;

        JsonParser parser = jsonFactory.createParser(request.buf, request.contentOffset, request.contentLen);

        boolean parseFailed = false;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            response.predefinedResponse = s400Close;
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if (name.equals("email")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                email = parser.getText();
            } else if (name.equals("first_name")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                firstName = parser.getText();
            } else if (name.equals("last_name")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                lastName = parser.getText();
            } else if (name.equals("gender")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                gender = parser.getText();
            } else if (name.equals("birth_date")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                birthDate = parser.getLongValue();
            } else if (name.equals("id")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                id = parser.getIntValue();
            } else {
                parseFailed = true;
                break;
            }
        }

        if (parseFailed || email == null || firstName == null || lastName == null || gender == null || birthDate == null || id == null) {
            response.predefinedResponse = s400Close;
            return;
        }

        byte genderByte;
        if (gender.equals("m"))
            genderByte = 0;
        else if (gender.equals("f"))
            genderByte = 1;
        else {
            response.predefinedResponse = s400Close;
            return;
        }

//                        if (birthDate < startBirth || birthDate >= endBirth) {
//                            response.predefinedResponse = any400;
//                            return;
//                        }

        Integer finalId = id;
        String finalEmail = email;
        String finalFirstName = firstName;
        String finalLastName = lastName;
        Long finalBirthDate = birthDate;
        response.afterResponseSent = () -> repository.addUser(new User(finalId, finalEmail, finalFirstName, finalLastName, genderByte, finalBirthDate));

        response.predefinedResponse = s200Close;
    }

    public void handleUpdateUser(User user, Request request, Response response) throws Exception {
        if (request.contentLen <= 0) {
            response.predefinedResponse = s400Close;
            return;
        }

        JsonParser parser = jsonFactory.createParser(request.buf, request.contentOffset, request.contentLen);

        String email = null, firstName = null, lastName = null, gender = null;
        Long birthDate = null;

        boolean parseFailed = false;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            response.predefinedResponse = s400Close;
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if (name.equals("email")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                email = parser.getText();
            } else if (name.equals("first_name")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                firstName = parser.getText();
            } else if (name.equals("last_name")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                lastName = parser.getText();
            } else if (name.equals("gender")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                gender = parser.getText();
            } else if (name.equals("birth_date")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                birthDate = parser.getLongValue();
            } else {
                parseFailed = true;
                break;
            }
        }

        if (parseFailed || (email == null && firstName == null && lastName == null && gender == null && birthDate == null)) {
            response.predefinedResponse = s400Close;
            return;
        }

                        /*
                        email, first_name, last_name, gender, birth_date
                         */

//                            if (email != null && email.length() > 100) {
//                                response.predefinedResponse = any400;
//                                return;
//                            }
//                            if (firstName != null && firstName.length() > 50) {
//                                response.predefinedResponse = any400;
//                                return;
//                            }
//                            if (lastName != null && lastName.length() > 50) {
//                                response.predefinedResponse = any400;
//                                return;
//                            }

        byte genderByte;
        if (gender != null) {
            if (gender.equals("m"))
                genderByte = 0;
            else if (gender.equals("f"))
                genderByte = 1;
            else {
                response.predefinedResponse = s400Close;
                return;
            }
        } else genderByte = -1;

//                        if (birthDate != null && (birthDate < startBirth || birthDate >= endBirth)) {
//                            response.predefinedResponse = any400;
//                            return;
//                        }

        boolean modifyAvg = false;

        if (email != null) {
            user.email = email;
        }
        if (firstName != null) {
            user.firstName = firstName;
        }
        if (lastName != null) {
            user.lastName = lastName;
        }
        if (genderByte != -1 && genderByte != user.gender) {
            user.gender = genderByte;
            modifyAvg = true;
        }
        if (birthDate != null && birthDate != user.birthDate) {
            user.birthDate = birthDate;
            modifyAvg = true;
        }

        user.bEntityCache = null;

        if (modifyAvg) {
            response.afterResponseSent = () -> {
                if (user.visitsCache != null) {
                    user.visitsCache.forEach(visit -> visit.locationRef.clearQueryCache());
                }
            };
        }

        response.predefinedResponse = s200Close;
    }

    public void handleGetVisit(Visit visit, Request request, Response response) throws Exception {

        try (JsonGenerator gen = jsonFactory.createGenerator(response.outputStream)) {
            gen.writeStartObject();

            gen.writeFieldName(ID_FIELD);
            gen.writeNumber(visit.id);

            gen.writeFieldName(LOCATION_FIELD);
            gen.writeNumber(visit.location);

            gen.writeFieldName(USER_FIELD);
            gen.writeNumber(visit.user);

            gen.writeFieldName(VISITED_AT_FIELD);
            gen.writeNumber(visit.visitedAt);

            gen.writeFieldName(MARK_FIELD);
            gen.writeNumber(visit.mark);

            gen.writeEndObject();
        }
    }

    public void handleUpdateVisit(Visit visit, Request request, Response response) throws Exception {
        if (request.contentOffset == -1) {
            response.predefinedResponse = s400Close;
            return;
        }

        Integer locationId = null, userId = null;
        Long visitedAt = null;
        Integer mark = null;

        JsonParser parser = jsonFactory.createParser(request.buf, request.contentOffset, request.contentLen);


        boolean parseFailed = false;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            response.predefinedResponse = s400Close;
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if (name.equals("location")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                locationId = parser.getIntValue();
            } else if (name.equals("user")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                userId = parser.getIntValue();
            } else if (name.equals("visited_at")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                visitedAt = parser.getLongValue();
            } else if (name.equals("mark")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                mark = parser.getIntValue();
            } else {
                parseFailed = true;
                break;
            }
        }

        if (parseFailed || (locationId == null && userId == null && visitedAt == null && mark == null)) {
            response.predefinedResponse = s400Close;
            return;
        }

//                        if (visitedAt != null && (visitedAt < startVisit || visitedAt >= endVisit)) {
//                            response.predefinedResponse = any400;
//                            return;
//                        }


        if (mark != null && (mark < 0 || mark > 5)) {
            response.predefinedResponse = s400Close;
            return;
        }

        Location location;
        if (locationId != null) {
            location = repository.getLocation(locationId);
            if (location == null) {
                response.predefinedResponse = s400Close;
                return;
            }
        } else location = null;

        User user;
        if (userId != null) {
            user = repository.getUser(userId);
            if (user == null) {
                response.predefinedResponse = s400Close;
                return;
            }
        } else user = null;

        Integer finalLocationId = locationId;
        Integer finalUserId = userId;
        Long finalVisitedAt = visitedAt;
        Integer finalMark = mark;
        response.afterResponseSent = () -> {
            if (finalLocationId != null && visit.location != finalLocationId) {
                Location oldLocationRef = visit.locationRef;
                oldLocationRef.clearQueryCache();
                visit.location = finalLocationId;
                visit.locationRef = location;
                location.clearQueryCache();
                repository.removeAndAddLocationCache(oldLocationRef, visit, location);
            }
            if (finalUserId != null && visit.user != finalUserId) {
                User oldUserRef = visit.userRef;
                oldUserRef.clearQueryCache();
                visit.user = finalUserId;
                visit.userRef = user;
                user.clearQueryCache();
                repository.removeAndAddUserCache(oldUserRef, visit, user);
            }
            if (finalVisitedAt != null && visit.visitedAt != finalVisitedAt) {
                visit.userRef.clearQueryCache();
                List<Visit> tree = visit.userRef.visitsCache;
                int index = Collections.binarySearch(tree, visit);
                if (index >= 0) tree.remove(index);
                visit.visitedAt = finalVisitedAt;
                tree.add(visit);
                tree.sort(Visit::compareTo);
            }
            if (finalMark != null) visit.mark = finalMark.byteValue();
        };

        visit.bEntityCache = null;

        response.predefinedResponse = s200Close;
    }

    public void handleNewVisit(Request request, Response response) throws Exception {
        if (request.contentOffset == -1) {
            response.predefinedResponse = s400Close;
            return;
        }
        Integer locationId = null, userId = null, id = null;
        Long visitedAt = null;
        Integer mark = null;

        JsonParser parser = jsonFactory.createParser(request.buf, request.contentOffset, request.contentLen);


        boolean parseFailed = false;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            response.predefinedResponse = s400Close;
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if (name.equals("location")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                locationId = parser.getIntValue();
            } else if (name.equals("user")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                userId = parser.getIntValue();
            } else if (name.equals("visited_at")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                visitedAt = parser.getLongValue();
            } else if (name.equals("mark")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                mark = parser.getIntValue();
            } else if (name.equals("id")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                id = parser.getIntValue();
            } else {
                parseFailed = true;
                break;
            }
        }

        if (parseFailed) {
            response.predefinedResponse = s400Close;
            return;
        }

        if (locationId == null || userId == null || visitedAt == null || mark == null || id == null) {
            response.predefinedResponse = s400Close;
            return;
        }

        if (mark < 0 || mark > 5) {
            response.predefinedResponse = s400Close;
            return;
        }

//                        if (visitedAt < startVisit || visitedAt >= endVisit) {
//                            response.predefinedResponse = any400;
//                            return;
//                        }


        Integer finalUserId = userId;
        Integer finalLocationId = locationId;
        Long finalVisitedAt = visitedAt;
        Integer finalMark = mark;
        Integer finalId = id;
        response.afterResponseSent = () -> {
            User user = repository.getUser(finalUserId);
            if (user == null) {
                log.error("update visit with invalid user {}", finalUserId);
                return;
            }
            Location location = repository.getLocation(finalLocationId);
            if (location == null) {
                log.error("update visit with invalid location {}", finalLocationId);
                return;
            }

            user.clearQueryCache();
            location.clearQueryCache();

            repository.addVisit(new Visit(finalId, finalLocationId, finalUserId, finalVisitedAt, finalMark), user, location);
        };

        response.predefinedResponse = s200Close;
    }

    public void handleNewLocation(Request request, Response response) throws Exception {
        if (request.contentOffset == -1) {
            response.predefinedResponse = s400Close;
            return;
        }
        String place = null, country = null, city = null;
        Long distance = null;
        Integer id = null;

        JsonParser parser = jsonFactory.createParser(request.buf, request.contentOffset, request.contentLen);

        boolean parseFailed = false;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            response.predefinedResponse = s400Close;
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if (name.equals("place")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                place = parser.getText();
            } else if (name.equals("country")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                country = parser.getText();
            } else if (name.equals("city")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                city = parser.getText();
            } else if (name.equals("distance")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                distance = parser.getLongValue();
            } else if (name.equals("id")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                id = parser.getIntValue();
            } else {
                parseFailed = true;
                break;
            }
        }

        if (parseFailed) {
            response.predefinedResponse = s400Close;
            return;
        }

        if (place == null || country == null || city == null || distance == null || id == null) {
            response.predefinedResponse = s400Close;
            return;
        }

//                        if (country.length() > 50 || city.length() > 50) {
//                            response.predefinedResponse = any400;
//                            return;
//                        }

//                        if (distance < 0) {
//                            response.predefinedResponse = any400;
//                            return;
//                        }

        Integer finalId = id;
        String finalPlace = place;
        String finalCountry = country;
        String finalCity = city;
        Long finalDistance = distance;
        response.afterResponseSent = () -> repository.addLocation(new Location(finalId, finalPlace, finalCountry, finalCity, finalDistance));

        response.predefinedResponse = s200Close;
    }

    public void handleGetLocation(Location location, Request request, Response response) throws Exception {
        try (JsonGenerator gen = jsonFactory.createGenerator(response.outputStream)) {
            gen.writeStartObject();

            gen.writeFieldName(ID_FIELD);
            gen.writeNumber(location.id);

            gen.writeFieldName(PLACE_FIELD);
            gen.writeString(location.place);

            gen.writeFieldName(COUNTRY_FIELD);
            gen.writeString(location.country);

            gen.writeFieldName(CITY_FIELD);
            gen.writeString(location.city);

            gen.writeFieldName(DISTANCE_FIELD);
            gen.writeNumber(location.distance);

            gen.writeEndObject();
        }

    }

    public void handleUpdateLocation(Location location, Request request, Response response) throws Exception {
        if (request.contentOffset == -1) {
            response.predefinedResponse = s400Close;
            return;
        }

        String place = null, country = null, city = null;
        Long distance = null;

        JsonParser parser = jsonFactory.createParser(request.buf, request.contentOffset, request.contentLen);


        boolean parseFailed = false;
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            response.predefinedResponse = s400Close;
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String name = parser.getCurrentName();
            if (name.equals("place")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                place = parser.getText();
            } else if (name.equals("country")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                country = parser.getText();
            } else if (name.equals("city")) {
                if (parser.nextToken() != JsonToken.VALUE_STRING) {
                    parseFailed = true;
                    break;
                }
                city = parser.getText();
            } else if (name.equals("distance")) {
                if (parser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                    parseFailed = true;
                    break;
                }
                distance = parser.getLongValue();
            } else {
                parseFailed = true;
                break;
            }
        }

        if (parseFailed || (place == null && country == null && city == null && distance == null)) {
            response.predefinedResponse = s400Close;
            return;
        }


//                            if (country != null && country.length() > 50) {
//                                response.predefinedResponse = any400;
//                                return;
//                            }
//
//                            if (city != null && city.length() > 50) {
//                                response.predefinedResponse = any400;
//                                return;
//                            }

        boolean modifyVisits = false;

        if (distance != null && distance < 0) {
            response.predefinedResponse = s400Close;
            return;
        }

        if (place != null && !place.equals(location.place)) {
            location.place = place;
            modifyVisits = true;
        }
        if (country != null && !country.equals(location.country)) {
            location.country = country;
            modifyVisits = true;
        }
        if (city != null) location.city = city;
        if (distance != null && distance != location.distance) {
            location.distance = distance;
            modifyVisits = true;
        }

        location.bEntityCache = null;

        if (modifyVisits) {
            response.afterResponseSent = () -> {
                if (location.visitsCache != null) {
                    location.visitsCache.forEach(visit -> visit.userRef.clearQueryCache());
                }
            };
        }

        response.predefinedResponse = s200Close;
    }

    public void handleAvg(Location location, Request request, Response response) throws Exception {
//GET /locations/<id>/avg для получения средней оценки достопримечательности

                    /*
                    fromDate - учитывать оценки только с visited_at > fromDate
toDate - учитывать оценки только с visited_at < toDate
fromAge - учитывать только путешественников, у которых возраст (считается от текущего timestamp) больше этого параметра
toAge - как предыдущее, но наоборот
gender - учитывать оценки только мужчин или женщин
                     */

        fromDate = null;
        toDate = null;
        fromAge = null;
        toAge = null;
        genderByte = -1;

        FastUriParser.parse(request, ((uri, start, eq, end) -> {
            if (FastUriParser.isParam(uri, start, fromDateName)) {
                int offset = eq + 1;
                int len = end - eq;
                if (!HttpUtil.isSignedNumeric(uri, offset, len)) {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                } else {
                    fromDate = HttpUtil.parseSignedLong(uri, offset, len);
                }
            } else if (FastUriParser.isParam(uri, start, toDateName)) {
                int offset = eq + 1;
                int len = end - eq;
                if (!HttpUtil.isSignedNumeric(uri, offset, len)) {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                } else {
                    toDate = HttpUtil.parseSignedLong(uri, offset, len);
                }
            } else if (FastUriParser.isParam(uri, start, fromAgeName)) {
                int offset = eq + 1;
                int len = end - eq;
                if (!HttpUtil.isNumeric(uri, offset, len)) {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                } else {
                    fromAge = HttpUtil.parseLong(uri, offset, len);
                }
            } else if (FastUriParser.isParam(uri, start, toAgeName)) {
                int offset = eq + 1;
                int len = end - eq;
                if (!HttpUtil.isNumeric(uri, offset, len)) {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                } else {
                    toAge = HttpUtil.parseLong(uri, offset, len);
                }
            } else if (FastUriParser.isParam(uri, start, genderName)) {
                int len = end - eq;
                if (len == 1) {
                    int offset = eq + 1;
                    byte sex = uri[offset];
                    if (sex == 'm')
                        genderByte = 0;
                    else if (sex == 'f')
                        genderByte = 1;
                    else {
                        response.predefinedResponse = request.keepalive ? s400 : s400Close;
                    }
                } else {
                    response.predefinedResponse = request.keepalive ? s400 : s400Close;
                }
            }
        }));

        if (response.predefinedResponse != null) {
            return;
        }

        List<Visit> stream = location.visitsCache == null ? Collections.emptyList() : location.visitsCache;

        long maxBirthday;
        if (fromAge != null) {
            maxBirthday = now.minusYears(fromAge).toEpochSecond();
        } else {
            maxBirthday = 0;
        }

        long minBirthday;
        if (toAge != null) {
            minBirthday = now.minusYears(toAge).toEpochSecond();
        } else {
            minBirthday = 0;
        }

        count = 0;
        totalMark = 0;
        stream.forEach(visit -> {
            if (fromDate != null) {
                if (visit.visitedAt <= fromDate) return;
            }
            if (toDate != null) {
                if (visit.visitedAt >= toDate) return;
            }

            if (fromAge != null) {
                if (visit.userRef.birthDate >= maxBirthday) return;
            }

            if (toAge != null) {
                if (visit.userRef.birthDate <= minBirthday) return;
            }

            if (genderByte != -1 && visit.userRef.gender != genderByte) return;

            count++;
            totalMark += visit.mark;
        });

        double avg = count == 0 ? 0 : ((double) totalMark) / count;

        avg = (double)Math.round(avg * 100000d) / 100000d;

        try (JsonGenerator gen = jsonFactory.createGenerator(response.outputStream)) {
            gen.writeStartObject();

            gen.writeFieldName(AVG_FIELD);
            gen.writeRawValue(String.valueOf(avg));

            gen.writeEndObject();
        }
    }
}
