package ru.chemist.highloadcup.warmup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Warmup {
    private static final Logger log = LoggerFactory.getLogger(Warmup.class);
    private final Client client = new Client();

    public void warmup() throws InterruptedException {
        int warmupThreads = 4;
        //todo post
        String[] entities = new String[] {"users", "locations", "visits", "avg", "list"};
        int requests = entities.length * 20000 * 3;
        int requestsPerThread = requests / warmupThreads;

        long start = System.currentTimeMillis();

        Thread[] threads = new Thread[warmupThreads];
        for(int j=0;j<threads.length;j++) {
            int finalJ = j;
            threads[j] = new Thread(() -> {
//                Affinity.setAffinity(finalJ);
                for(int i=0;i<requestsPerThread;i++) {
                    String data;
                    try {
                        long userId = 1 + i % 100;
                        String entity = entities[i % entities.length];
                        if (entity.equals("avg")) {
                            data = client.get("/locations/" + userId + "/avg?fromDate=0&toDate=1223268286&fromAge=10&toAge=100&gender=f");
                        } else if (entity.equals("list")) {
                            data = client.get("/users/" + userId + "/visits?fromDate=0&toDate=1223268286&toDistance=99999");
                        } else {
                            data = client.get("/" + entity + "/" + userId);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Error at step " + i + ".", e);
                    }
                    if (!data.startsWith("{")) throw new RuntimeException("Error at step " + i + ". Data: " + data);
                }
            });
            threads[j].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        log.info("total time: {} ms", System.currentTimeMillis() - start);
    }
}
