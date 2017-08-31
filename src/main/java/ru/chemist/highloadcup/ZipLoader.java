package ru.chemist.highloadcup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipLoader {
    private static final Logger log = LoggerFactory.getLogger(ZipLoader.class);

    static void loadZip(Repository repository) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        try (ZipFile zipFile = new ZipFile("/tmp/data/data.zip")) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            TreeMap<Entry, InputStream> streams = new TreeMap<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                streams.put(new Entry(entry.getName()), zipFile.getInputStream(entry));
            }

            for (Map.Entry<Entry, InputStream> entry : streams.entrySet()) {
                String name = entry.getKey().filename;
                log.debug("load {}", name);

                try {
                    //order: l, u, v (asc)
                    if (name.startsWith("users_")) {
                        JsonNode node = objectMapper.readTree(entry.getValue());
                        parseUsers(repository, node);
                    } else if (name.startsWith("locations_")) {
                        JsonNode node = objectMapper.readTree(entry.getValue());
                        parseLocations(repository, node);
                    } else if (name.startsWith("visits_")) {
                        JsonNode node = objectMapper.readTree(entry.getValue());
                        parseVisits(repository, node);
                    }
                } finally {
                    entry.getValue().close();
                }
            }
        }

        List<String> options = Files.readAllLines(Paths.get("/tmp/data/options.txt"));
        repository.now = Long.parseLong(options.get(0));
        System.out.println("now "+repository.now);
    }

    private static void parseUsers(Repository repository, JsonNode node) {
        /*
         {
            "id": 1,
            "email": "robosen@icloud.com",
            "first_name": "Данила",
            "last_name": "Стамленский",
            "gender": "m",
            "birth_date": 345081600,
        }
         */
        node = node.get("users");
        for (JsonNode userNode : node) {
            long birthDate = userNode.get("birth_date").asLong();
            int userId = userNode.get("id").asInt();
            repository.addUser(new User(
                    userId,
                    userNode.get("email").asText(),
                    userNode.get("first_name").asText(),
                    userNode.get("last_name").asText(),
                    userNode.get("gender").asText().equals("m") ? 0 : (byte) 1,
                    birthDate
            ));
        }
    }

    private static void parseVisits(Repository repository, JsonNode node) {
        /*
         {"user": 44, "location": 32, "visited_at": 1103485742, "id": 1, "mark": 4}
         */
        node = node.get("visits");
        for (JsonNode visitNode : node) {
            long visitedAt = visitNode.get("visited_at").asLong();
            int userId = visitNode.get("user").asInt();
            User user = repository.getUser(userId);
            if (user == null) throw new RuntimeException("User "+userId+" not found");
            int locationId = visitNode.get("location").asInt();
            Location location = repository.getLocation(locationId);
            if (location == null) throw new RuntimeException("Location "+locationId+" not found");

            repository.addVisit(new Visit(
                    visitNode.get("id").asInt(),
                    locationId,
                    userId,
                    visitedAt,
                    visitNode.get("mark").asInt()
            ), user, location);
        }
    }

    private static void parseLocations(Repository repository, JsonNode node) {
        /*
         {"distance": 6, "city": "Москва", "place": "Набережная", "id": 1, "country": "Аргентина"}
         */
        node = node.get("locations");
        for (JsonNode locationNode : node) {
            int locationId = locationNode.get("id").asInt();
            repository.addLocation(new Location(
                    locationId,
                    locationNode.get("place").asText(),
                    locationNode.get("country").asText(),
                    locationNode.get("city").asText(),
                    locationNode.get("distance").asLong()
            ));
        }
    }

    private static class Entry implements Comparable<Entry> {
        public final String filename;
        private final String prefix;
        private final int num;

        public Entry(String filename) {
            this.filename = filename;
            String[] parts = filename.substring(0, filename.indexOf('.')).split("_");
            this.prefix = parts[0];
            this.num = Integer.parseInt(parts[1]);
        }

        @Override
        public int compareTo(Entry e) {
            int r = prefix.compareTo(e.prefix);
            return r == 0 ? Integer.compare(num, e.num) : r;
        }
    }
}
