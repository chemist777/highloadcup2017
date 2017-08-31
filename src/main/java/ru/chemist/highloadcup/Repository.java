package ru.chemist.highloadcup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public class Repository {
    private static final Logger log = LoggerFactory.getLogger(Repository.class);

//    public final Map<Long, User> users = Server.THREADS == 1 ? new HashMap<>(1000) : new ConcurrentHashMap<>(1000, 0.75f, Server.THREADS);
//    public final Map<Long, Visit> visits = Server.THREADS == 1 ? new HashMap<>(1000) : new ConcurrentHashMap<>(1000, 0.75f, Server.THREADS);
//    public final Map<Long, Location> locations = Server.THREADS == 1 ? new HashMap<>(1000) : new ConcurrentHashMap<>(1000, 0.75f, Server.THREADS);;

    private volatile User[] users = new User[1_100_000];
    private volatile Visit[] visits = new Visit[10_100_000];
    private volatile Location[] locations = new Location[1_000_000];

    public long now;

    public User getUser(int id) {
        return id >= users.length ? null : users[id];
    }

    public Visit getVisit(int id) {
        return id >= visits.length ? null : visits[id];
    }

    public Location getLocation(int id) {
        return id >= locations.length ? null : locations[id];
    }

    public void addUser(User user) {
        if (user.id >= users.length) {
            log.warn("users array resize from {} to {}", users.length, user.id + 1);
            users = Arrays.copyOf(users, user.id + 1);
        }
        users[user.id] = user;
    }

    /**
     * Should be called last.
     * @param visit
     */
    public void addVisit(Visit visit, User user, Location location) {
        if (visit.id >= visits.length) {
            log.warn("visits array resize from {} to {}", visits.length, visit.id + 1);
            visits = Arrays.copyOf(visits, visit.id + 1);
        }
        visits[visit.id] = visit;

        visit.userRef = user;
        visit.locationRef = location;

        synchronized (user) {
            addTreeCache(user, visit);
        }
        synchronized (location) {
            addCache(location, visit);
        }
    }

    public void removeAndAddUserCache(User oldUser, Visit visit, User newUser) {
        synchronized (oldUser) {
            List<Visit> oldVisits = oldUser.visitsCache;
            int index = Collections.binarySearch(oldVisits, visit);
            if (index >= 0) oldVisits.remove(index);
        }

        synchronized (newUser) {
            addTreeCache(newUser, visit);
        }
    }

    public void removeAndAddLocationCache(Location oldLocation, Visit visit, Location newLocation) {
        synchronized (oldLocation) {
            List<Visit> oldVisits = oldLocation.visitsCache;
            removeIf(oldVisits, visitObj -> visitObj == visit);
        }
        synchronized (newLocation) {
            addCache(newLocation, visit);
        }
    }

    private static void removeIf(Collection<Visit> list, Predicate<Visit> predicate) {
        if (list == null || list.size() == 0) return;
        Iterator<Visit> it = list.iterator();
        while (it.hasNext()) {
            if (predicate.test(it.next())) {
                it.remove();
                break;
            }
        }
    }

    public void addLocation(Location location) {
        if (location.id >= locations.length) {
            log.warn("locations array resize from {} to {}", locations.length, location.id + 1);
            locations = Arrays.copyOf(locations, location.id + 1);
        }
        locations[location.id] = location;
    }

    private static void addCache(Location location, Visit val) {
        List<Visit> list = location.visitsCache;
        if (list == null) {
            list = new ArrayList<>();
            list.add(val);
            location.visitsCache = list;
        } else {
            list.add(val);
        }
    }

    private static void addTreeCache(User user, Visit val) {
        List<Visit> list = user.visitsCache;
        if (list == null) {
            list = new ArrayList<>();
            list.add(val);
            user.visitsCache = list;
        } else {
            list.add(val);
            list.sort(Visit::compareTo);
        }
    }

}
