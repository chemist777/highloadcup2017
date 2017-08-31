package ru.chemist.highloadcup;


import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.chemist.highloadcup.jni.NativeNet;
import ru.chemist.highloadcup.warmup.Warmup;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.chemist.highloadcup.HttpUtil.makeResponse;
import static ru.chemist.highloadcup.ZipLoader.loadZip;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final Repository repository = new Repository();
    public static final int PORT = Integer.parseInt(System.getenv("SERVER_PORT"));
    public static final int THREADS = 8;
    private static volatile boolean CACHE_ENABLED = true;

    private long serverSocket;
    private NativeNet serverNet;
    private Thread[] workerThreads;
    private volatile boolean shutdown;

    private int[] cacheHits;
    private int[] cacheMisses;
    private final ByteBufferPool responseBufferPools = new ConcurrentByteBufferPool();
    private final BytePool requestBufferPools = new ConcurrentBytePool();

    private NativeNet[] net;

    //    private Thread keepAliveThread;
//    private LastActivity[] lastActivities;
//    private static final long EXPIRATION_MS = TimeUnit.MINUTES.toMillis(5);
    private long directLimitPerThread;
    private long heapLimitPerThread;
    private final int[] cons = new int[THREADS];
    private final int[] reqs = new int[THREADS];
    private final Queue<Connection> sharedConnectionPool = new LinkedBlockingQueue<>();

    public void start() throws Exception {
        NativeNet.init();
        loadZip(repository);

        for(int i=0;i<1000;i++) sharedConnectionPool.add(new Connection());

        workerThreads = new Thread[THREADS];
        cacheHits = new int[workerThreads.length];
        cacheMisses = new int[workerThreads.length];
        net = new NativeNet[THREADS];

        serverNet = new NativeNet();
        serverSocket = serverNet.bind(PORT);

        long directLimit = sun.misc.VM.maxDirectMemory();
        System.out.println("Direct limit: "+directLimit);
        directLimitPerThread = (long) (0.9*directLimit);
        heapLimitPerThread = (1800 * 1024 * 1024);

        for(int i=0;i<workerThreads.length;i++) {
//            lastActivities[i] = new LastActivity();
            net[i] = new NativeNet();

            int finalI = i;
            workerThreads[i] = new Thread(() -> {
                try {
//                    Affinity.setAffinity(finalI);
                    /*
                    check affinity:

                    pname="java"  # for example
for pid in $(pgrep "${pname}")
do
    [ "${pid}" != "" ] || exit
    echo "PID: ${pid}"
    for tid in \
      $(ps --no-headers -ww -p "${pid}" -L -olwp | sed 's/$/ /' | tr  -d '\n')
    do
    taskset -cp "${tid}"   # substitute thread id in place of a process id
    done
done
                     */
                    NativeNet myNet = net[finalI];
                    assert myNet != null;

                    long epollId = NativeNet.epollCreate();
                    NativeNet.epollListen(epollId, serverSocket);

                    RequestHandler handler = new RequestHandler(repository);

                    Queue<Connection> connectionPool = new ArrayDeque<>();
                    for(int z=0;z<2000/THREADS;z++) {
                        connectionPool.add(new Connection());
                    }

                    Map<Long, Connection> connections = new HashMap<>(300);
                    ByteBuffer events = ByteBuffer.allocateDirect(4096 * (4 + 1));
                    byte[] eventsBytes = new byte[events.capacity()];

                    long eventsAddress = BytesUtil.getAddress(events);
                    long eventsPointer = NativeNet.eventsBuffer();

//                    long nextCheck = 0;

                    ByteBuffer directResponseBuffer = ByteBuffer.allocateDirect(32 * 1024);

                    int len;
                    int pos;
                    byte type;
                    long fd;

                    while (!shutdown) {

                        int count = NativeNet.getEvents(epollId, serverSocket, eventsAddress, eventsPointer);
                        if (count == 0) {
//                            long now = System.currentTimeMillis();
//                            if (nextCheck <= now) {
//                                long expireTime = now - EXPIRATION_MS;
//                                for (Iterator<Map.Entry<Long, Connection>> iterator = connections.entrySet().iterator(); iterator.hasNext(); ) {
//                                    Map.Entry<Long, Connection> entry = iterator.next();
//                                    Connection connection = entry.getValue();
//                                    long lastActivityMs = connection.lastActivityMs;
//                                    if (lastActivityMs < expireTime) {
//                                        connection.reset();
//                                        myNet.close(entry.getKey());
//                                        connectionPool.add(connection);
//                                        iterator.remove();
//                                    }
//                                }
//                                nextCheck = now + EXPIRATION_MS;
//                            }

                            continue;
                        }

                        len = count * (4+1);
                        events.rewind();
                        events.get(eventsBytes, 0, len);
                        pos = 0;
                        while (pos < len) {
                            type = eventsBytes[pos++];
                            fd = byteArrayToInt(eventsBytes, pos);
                            pos += 4;

                            if (type == 0) {
                                //try accept new connection
                                Connection newConnection = connectionPool.poll();
                                if (newConnection == null) {
                                    newConnection = sharedConnectionPool.poll();
                                    if (newConnection == null) newConnection = new Connection();
                                }
                                connections.put(fd, newConnection);
                                cons[finalI]++;
                            } else {
                                processConnection(connections, fd, myNet, finalI, handler, connectionPool, directResponseBuffer);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!shutdown) log.error("error in worker thread " + finalI, e);
                }
            });
        }

        for (Thread workerThread : workerThreads) {
            workerThread.setPriority(Thread.MAX_PRIORITY);
            workerThread.start();
        }

//        keepAliveThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                log.error("Can't stop server", e);
            }
        }));
        log.info("Server started on {}", PORT);

        Runtime runtime = Runtime.getRuntime();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
//        executorService.execute(() -> Affinity.setAffinity(3));
        executorService.scheduleWithFixedDelay(() -> {
            System.out.println("mem used: "+(runtime.totalMemory() - runtime.freeMemory())+", hit: "+Arrays.stream(cacheHits).sum()+", miss: " + Arrays.stream(cacheMisses).sum() +", cons: " + Arrays.toString(cons) + ", reqs: " + Arrays.toString(reqs));

            for(int i=0;i<ByteBufferPool.BUCKETS;i++) {
                int maxOverflow = 0;
                ByteBufferPool pool = responseBufferPools;
                if (pool.overflow(i) > maxOverflow) maxOverflow = pool.overflow(i);
                if (maxOverflow > 0) {
                    System.out.println("No free response buffer with size " + i + ". Wants " + (ConcurrentByteBufferPool.prealloc[i] + maxOverflow) + " buffers.");
                }
            }

            for(int i=0;i<BytePool.BUCKETS;i++) {
                int maxOverflow = 0;
                BytePool pool = requestBufferPools;
                if (pool.overflow(i) > maxOverflow) maxOverflow = pool.overflow(i);
                if (maxOverflow > 0) {
                    System.out.println("No free request buffer with size " + i + ". Wants " + (ConcurrentBytePool.prealloc[i] + maxOverflow) + " buffers.");
                }
            }
        }, 15000, 15000, TimeUnit.MILLISECONDS);
    }

    public static int byteArrayToInt(byte[] b, int offset)
    {
        return   b[offset + 3] & 0xFF |
                (b[offset + 2] & 0xFF) << 8 |
                (b[offset + 1] & 0xFF) << 16 |
                (b[offset] & 0xFF) << 24;
    }

    private void processConnection(Map<Long, Connection> connections, long socket, NativeNet myNet, int finalI, RequestHandler handler, Queue<Connection> connectionPool, ByteBuffer defaultResponseBuffer) {
        Connection connection = connections.get(socket);
        if (connection == null) {
//            log.error("connection {} not found", socket);
            return;
        }
        Request request = connection.request;
        Response response = connection.response;

        int readResult = ReadResult.CLOSE;
        try {
            readResult = HttpUtil.readRequest(myNet, socket, request);
            if (readResult != ReadResult.READY) return;

            reqs[finalI]++;

            //non-null if we want to save to cache
            CacheHolder entity = null;
            //non-null if we want to save to query cache
            CacheKey queryCacheKey = null;

            boolean cacheHit = false, cacheMiss = false;

            //fast path, cache & 404 handling
            if (request.action == Action.NOT_FOUND) {
                response.predefinedResponse = request.keepalive ? handler.s404 : handler.s404Close;
                response.predefinedResponse.rewind();
            } else if (request.method != HttpMethod.GET && request.method != HttpMethod.POST) {
                response.predefinedResponse = request.keepalive ? handler.s400 : handler.s400Close;
                response.predefinedResponse.rewind();
            } else {
                //default status
                response.status = request.keepalive ? Status.OK : Status.OK_NOKEEP;

                //check cache
                if (request.entity == Entity.USERS) {
                    if (request.action == Action.NEW) {
                        handler.handleNewUser(request, response);
                    } else {
                        User user = repository.getUser(request.id);
                        if (user == null) {
                            response.predefinedResponse = request.keepalive ? handler.s404 : handler.s404Close;
                            response.predefinedResponse.rewind();
                        } else {
                            //check cache
                            if (request.action == Action.ENTITY) {
                                if (request.method == HttpMethod.GET) {
                                    ByteBuffer result = user.bEntityCache;
                                    if (CACHE_ENABLED && result != null) {
                                        response.predefinedResponse = result.duplicate();
                                        cacheHit = true;
                                    } else {
                                        handler.handleGetUser(user, request, response);
                                        cacheMiss = true;
                                        entity = user;
                                    }
                                } else {
                                    //update
                                    handler.handleUpdateUser(user, request, response);
                                }
                            } else if (request.action == Action.VISITS) {
                                queryCacheKey = new CacheKey();
                                //query string as cache key
                                queryCacheKey.init(request.buf, request.pathEnd + 1, request.uriEnd - request.pathEnd);

                                ByteBuffer result = user.get(queryCacheKey);
                                if (CACHE_ENABLED && result != null) {
                                    response.predefinedResponse = result.duplicate();
                                    cacheHit = true;
                                } else {
                                    handler.handleGetUserVisits(user, request, response);
                                    cacheMiss = true;
                                    entity = user;
                                }
                            } else {
                                response.predefinedResponse = request.keepalive ? handler.s404 : handler.s404Close;
                                response.predefinedResponse.rewind();
                            }
                        }
                    }
                } else if (request.entity == Entity.LOCATIONS) {
                    if (request.action == Action.NEW) {
                        handler.handleNewLocation(request, response);
                    } else {
                        Location user = repository.getLocation(request.id);
                        if (user == null) {
                            response.predefinedResponse = request.keepalive ? handler.s404 : handler.s404Close;
                            response.predefinedResponse.rewind();
                        } else {
                            //check cache
                            if (request.action == Action.ENTITY) {
                                if (request.method == HttpMethod.GET) {
                                    ByteBuffer result = user.bEntityCache;
                                    if (CACHE_ENABLED && result != null) {
                                        response.predefinedResponse = result.duplicate();
                                        cacheHit = true;
                                    } else {
                                        handler.handleGetLocation(user, request, response);
                                        cacheMiss = true;
                                        entity = user;
                                    }
                                } else {
                                    //update
                                    handler.handleUpdateLocation(user, request, response);
                                }
                            } else if (request.action == Action.AVG) {
                                queryCacheKey = new CacheKey();
                                //query string as cache key
                                queryCacheKey.init(request.buf, request.pathEnd + 1, request.uriEnd - request.pathEnd);

                                ByteBuffer result = user.get(queryCacheKey);
                                if (CACHE_ENABLED && result != null) {
                                    response.predefinedResponse = result.duplicate();
                                    cacheHit = true;
                                } else {
                                    handler.handleAvg(user, request, response);
                                    cacheMiss = true;
                                    entity = user;
                                }
                            } else {
                                response.predefinedResponse = request.keepalive ? handler.s404 : handler.s404Close;
                                response.predefinedResponse.rewind();
                            }
                        }
                    }
                } else {
                    //VISITS
                    if (request.action == Action.NEW) {
                        handler.handleNewVisit(request, response);
                    } else {
                        Visit user = repository.getVisit(request.id);
                        if (user == null) {
                            response.predefinedResponse = request.keepalive ? handler.s404 : handler.s404Close;
                            response.predefinedResponse.rewind();
                        } else {
                            //check cache
                            if (request.action == Action.ENTITY) {
                                if (request.method == HttpMethod.GET) {
                                    ByteBuffer result = user.bEntityCache;
                                    if (CACHE_ENABLED && result != null) {
                                        response.predefinedResponse = result.duplicate();
                                        cacheHit = true;
                                    } else {
                                        handler.handleGetVisit(user, request, response);
                                        cacheMiss = true;
                                        entity = user;
                                    }
                                } else {
                                    //update
                                    handler.handleUpdateVisit(user, request, response);
                                }
                            } else {
                                response.predefinedResponse = request.keepalive ? handler.s404 : handler.s404Close;
                                response.predefinedResponse.rewind();
                            }
                        }
                    }
                }
            }

            ByteBuffer buf;

            if (response.predefinedResponse != null) {
                buf = response.predefinedResponse;
            } else {
                int offset = makeResponse(response.status, response.content, response.contentLength, response.buf);

                if (CACHE_ENABLED && cacheMiss) {
                    buf = responseBufferPools.get(offset);
                } else {
                    buf = defaultResponseBuffer;
                    buf.clear();
                }

                buf.put(response.buf, 0, offset);
                buf.flip();
            }

            if (responseBufferPools.totalBytesAllocated() > directLimitPerThread) {
                log.error("No direct memory");
            } else if (requestBufferPools.totalBytesAllocated() > heapLimitPerThread) {
                log.error("No heap memory");
            }

            HttpUtil.sendResponse(myNet, socket, buf);
            buf.rewind();

            if (response.afterResponseSent != null) response.afterResponseSent.run();

            if (cacheMiss) {
                cacheMisses[finalI]++;
                if (CACHE_ENABLED) {
                    if (queryCacheKey == null) {
                        //entity cache
                        entity.setEntityCache(buf);
                    } else {
                        queryCacheKey.makeImmutable(requestBufferPools);
                        entity.setQueryCache(queryCacheKey, buf);
                    }
                }
            } else if (cacheHit) {
                cacheHits[finalI]++;
            }
        } catch (JsonProcessingException e) {
            response.predefinedResponse = request.keepalive ? handler.s400 : handler.s400Close;
        } catch (Exception e) {
            log.error("error in request process", e);
            response.status = request.keepalive ? Status.INTERNAL_SERVER_ERROR : Status.INTERNAL_SERVER_ERROR_NOKEEP;
        } finally {
            if (readResult != ReadResult.NOT_READY) {
                if (!request.keepalive) {
                    connection.reset();
                    //close with RST
                    myNet.close(socket);
                    connections.remove(socket);
                    connectionPool.add(connection);
                } else {
                    //continue read from socket
                    request.reset();
                    response.reset();
                }
            }
        }
    }

    public void stop() throws Exception {
        if (shutdown) return;
        shutdown = true;

        serverNet.unbind(serverSocket);

        for (Thread workerThread : workerThreads) {
            workerThread.interrupt();
        }

//        keepAliveThread.interrupt();

        for (Thread workerThread : workerThreads) {
            workerThread.join();
        }
//        keepAliveThread.join();
        log.info("Server stopped");
    }

    public static void main(String[] args) throws Exception {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);

        ProcessBuilder pb = new ProcessBuilder("uname", "-a");
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.start();

        log.info("CPU cores count: {}", Runtime.getRuntime().availableProcessors());

        Server server = new Server();
        server.start();
        try {
            Server.CACHE_ENABLED = false;
            Warmup warmup = new Warmup();
            warmup.warmup();
            System.gc();
            warmup.warmup();
            Server.CACHE_ENABLED = true;
            Arrays.fill(server.cacheHits, 0);
            Arrays.fill(server.cacheMisses, 0);
            Arrays.fill(server.cons, 0);
            Arrays.fill(server.reqs, 0);
            System.gc();
            System.gc();
            System.gc();
            log.info("Warmed up");
        } catch (Exception e) {
            log.error("error at warmup", e);
            server.stop();
        }
    }
}
