package ru.chemist.highloadcup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.chemist.highloadcup.jni.NativeNet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HttpUtil {
    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);
    private static final byte[] CONTENT_LENGTH_O = "Content-Length: 0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] CONTENT_LENGTH_PREFIX = "Content-Length: ".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] HEADERS_END = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] USERS_ENTITY = "users".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] LOCATIONS_ENTITY = "locations".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] VISITS_ENTITY = "visits".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] NEW_ACTION = "new".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] AVG_ACTION = "avg".getBytes(StandardCharsets.ISO_8859_1);
//    public static volatile boolean KEEPALIVE_DETECTED;

    public static int readRequest(NativeNet net, long socket, Request request) throws IOException {
        //read in non-blocking mode
        int len = net.read(socket, request.directBufAddress, request.directBuf.capacity(), request.directBuf.position());

        if (len == -100)
            return ReadResult.NOT_READY;
        else if (len <= 0) {
            request.keepalive = false;
            return ReadResult.CLOSE;
        } else {
//            connection.lastActivityMs = System.currentTimeMillis();

            int newBufSize = request.directBuf.position() + len;
            request.directBuf.position(newBufSize);

            if (request.expectedRequestSize == -1 || newBufSize >= request.expectedRequestSize) {
                //copy memory for each packet until we found content-length or for the whole remaining part
                request.directBuf.position(request.bufSize);
                request.directBuf.get(request.buf, request.bufSize, newBufSize - request.bufSize);
                request.bufSize = newBufSize;
                request.directBuf.position(newBufSize);
            }

            if (tryParse(request)) {
                return ReadResult.READY;
            }

            return ReadResult.NOT_READY;
        }
    }

    public static boolean tryParse(Request request) {
        byte[] buf = request.buf;
        int bufSize = request.bufSize;

        if (request.expectedRequestSize != -1) {
            if (bufSize >= request.expectedRequestSize) {
                //if we have all content for POST request
                parseRequest(request);
                return true;
            } else {
                //isn't completed POST request
                //don't reparse headers
                return false;
            }
        }

//
//        GET /users/1077/visits HTTP/1.1
//Host: 127.0.0.1:8080
//Connection: keep-alive
//Cache-Control: max-age=0
//Upgrade-Insecure-Requests: 1
//User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.78 Safari/537.36
//Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
//        Accept-Encoding: gzip, deflate, br
//        Accept-Language: en-US,en;q=0.8,ru;q=0.6
        try {
            //read status line

            int method = -1;
            int uriStart = -1;
            int uriEnd = -1;
            int headersEndPos = -1;
            int methodLen = -1;
            byte currentByte, firstByte;

            for(int i = 0; i < bufSize - 3; i++) {
                currentByte = buf[i];
                if (currentByte == ' ') {
                    if (method == -1) {
                        //method ends
                        methodLen = i;
                        firstByte = buf[0];
                        if (firstByte == 'G')
                            method = HttpMethod.GET;
                        else if (firstByte == 'P')
                            method = HttpMethod.POST;
                        else
                            method = HttpMethod.UNSUPPORTED;
                    } else if (uriStart == -1) {
                        //path ends
                        assert methodLen > 0;
                        uriStart = methodLen + 1;
                        uriEnd = i - 1;
                    }
                } else if (currentByte == '\r' && buf[i + 1] == '\n' && buf[i + 2] == '\r' && buf[i + 3] == '\n') {
                    headersEndPos = i;
                    break;
                }
            }
            if (headersEndPos < 0 || method == -1 || uriStart == -1) {
                return false;
            }

            if (method == HttpMethod.POST) {
                request.keepalive = false;
                int contentLen = -1;
                int spacePos;
                int endPos;
                //"Content-Length: 3"
                for(int i = uriEnd + 1; i < headersEndPos - 16; i++) {
                    if (Character.toLowerCase(buf[i]) == 'c' &&
                            Character.toLowerCase(buf[i+1]) == 'o' &&
                            Character.toLowerCase(buf[i+2]) == 'n' &&
                            Character.toLowerCase(buf[i+3]) == 't' &&
                            Character.toLowerCase(buf[i+4]) == 'e' &&
                            Character.toLowerCase(buf[i+5]) == 'n' &&
                            Character.toLowerCase(buf[i+6]) == 't' &&
                            Character.toLowerCase(buf[i+7]) == '-' &&
                            Character.toLowerCase(buf[i+8]) == 'l') {
                        //find space
                        spacePos=i+15;
                        while (buf[spacePos] != ' ') spacePos++;
                        endPos = spacePos + 2;
                        while (endPos < headersEndPos && buf[endPos] != '\r') endPos++;
                        contentLen = parseInt(buf, spacePos + 1, endPos - (spacePos + 1));
                        break;
                    }
                }

                if (contentLen < 0) {
                    //no content length found
                    request.method = method;
                    request.uriStart = uriStart;
                    request.uriEnd = uriEnd;
                    parseRequest(request);
                    return true;
                }
                int expectedRequestSize = headersEndPos + 4 + contentLen;

                headersEndPos += 4;
                request.contentOffset = headersEndPos;
                request.contentLen = contentLen;

                if (bufSize < expectedRequestSize) {
                    request.method = method;
                    request.uriStart = uriStart;
                    request.uriEnd = uriEnd;
                    request.expectedRequestSize = expectedRequestSize;
                    //not enough content
                    return false;
                }
            } else if (method == HttpMethod.GET) {
                //Close
//                for(int i = uriEnd + 1; i <= headersEndPos - 5; i++) {
//                    if (Character.toLowerCase(buf[i]) == 'c' &&
//                            Character.toLowerCase(buf[i+1]) == 'l' &&
//                            Character.toLowerCase(buf[i+2]) == 'o' &&
//                            Character.toLowerCase(buf[i+3]) == 's' &&
//                            Character.toLowerCase(buf[i+4]) == 'e'
//                            ) {
//                        request.keepalive = false;
//                        break;
//                    }
//                }
                //tank always sends keep alive get requests, so we shouldn't try to search close here


//                if (request.keepalive) {
//                    KEEPALIVE_DETECTED = true;
//                }
            } else {
                request.keepalive = false;
            }

            request.method = method;
            request.uriStart = uriStart;
            request.uriEnd = uriEnd;
            parseRequest(request);
            return true;
        } catch (Exception e) {
            log.error("can't parse", e);
            return false;
        }
    }

    private static void parseRequest(Request request) {
        FastUriParser.parseUri(request);

        //search for entity name end
        int slashPos = pathIndexOf(request, (byte) '/', 1);
        int secondSlashPos = slashPos;
        if (slashPos >= 0) {
            int entityOffset = request.pathStart + 1;
            int entityLen = slashPos - request.pathStart - 1;

            if (equals(request.buf, entityOffset, entityLen, USERS_ENTITY)) {
                request.entity = Entity.USERS;
                slashPos = pathIndexOf(request, (byte) '/', 2 + USERS_ENTITY.length);
                if (slashPos < 0) {
                    //it's new or id or 404
                    int idOffset = secondSlashPos + 1;
                    int idLen = request.pathEnd - idOffset + 1;
                    if (isNumeric(request.buf, idOffset, idLen)) {
                        request.id = parseInt(request.buf, idOffset, idLen);
                        request.action = Action.ENTITY;
                    } else if (equals(request.buf, idOffset, idLen, NEW_ACTION)) {
                        request.action = Action.NEW;
                    }
                    //404
                } else {
                    //it's visits or 404
                    int idOffset = secondSlashPos + 1;
                    int idLen = slashPos - idOffset;
                    if (isNumeric(request.buf, idOffset, idLen) && equals(request.buf, slashPos + 1, request.pathEnd - slashPos, VISITS_ENTITY)) {
                        request.id = parseInt(request.buf, idOffset, idLen);
                        request.action = Action.VISITS;
                    }
                    //404
                }
            } else if (equals(request.buf, entityOffset, entityLen, VISITS_ENTITY)) {
                request.entity = Entity.VISITS;
                //it's new or id or 404
                int idOffset = slashPos + 1;
                int idLen = request.pathEnd - idOffset + 1;
                if (isNumeric(request.buf, idOffset, idLen)) {
                    request.id = parseInt(request.buf, idOffset, idLen);
                    request.action = Action.ENTITY;
                } else if (equals(request.buf, idOffset, idLen, NEW_ACTION)) {
                    request.action = Action.NEW;
                }
            } else if (equals(request.buf, entityOffset, entityLen, LOCATIONS_ENTITY)) {
                request.entity = Entity.LOCATIONS;
                slashPos = pathIndexOf(request, (byte) '/', 2 + LOCATIONS_ENTITY.length);
                if (slashPos < 0) {
                    //it's new or id or 404
                    int idOffset = secondSlashPos + 1;
                    int idLen = request.pathEnd - idOffset + 1;
                    if (isNumeric(request.buf, idOffset, idLen)) {
                        request.id = parseInt(request.buf, idOffset, idLen);
                        request.action = Action.ENTITY;
                    } else if (equals(request.buf, idOffset, idLen, NEW_ACTION)) {
                        request.action = Action.NEW;
                    }
                    //404
                } else {
                    //it's avg or 404
                    int idOffset = secondSlashPos + 1;
                    int idLen = slashPos - idOffset;
                    if (isNumeric(request.buf, idOffset, idLen) && equals(request.buf, slashPos + 1, request.pathEnd - slashPos, AVG_ACTION)) {
                        request.id = parseInt(request.buf, idOffset, idLen);
                        request.action = Action.AVG;
                    }
                    //404
                }
            }
        }
    }

    public static boolean isSignedNumeric(byte[] arr, int offset, int len) {
        if (len <= 0) return false;
        if (arr[offset] == '-') {
            offset++;
            len--;
            return isNumeric(arr, offset, len);
        } else {
            return isNumeric(arr, offset, len);
        }
    }

    public static boolean isNumeric(byte[] arr, int offset, int len) {
        if (len <= 0) return false;
        for(int i=offset;i<offset+len;i++) {
            byte b = arr[i];
            if (b < '0' || b > '9') return false;
        }
        return true;
    }

    public static int pathIndexOf(Request request, byte b, int startOffset) {
        for(int i=request.pathStart + startOffset;i<=request.pathEnd;i++) {
            if (request.buf[i] == b) return i;
        }
        return -1;
    }

    public static boolean equals(byte[] arr, int offset, int len, byte[] arr2) {
        return equals(arr, offset, len, arr2, 0, arr2.length);
    }

    public static boolean equals(byte[] arr, int offset, int len, byte[] arr2, int offset2, int len2) {
        if (len != len2) return false;
        for(int i=0;i<len;i++) {
            if (arr[offset+i]!=arr2[offset2+i]) return false;
        }
        return true;
    }

    public static int makeResponse(Status status, byte[] content, int contentLength, byte[] output) {
         /*
        HTTP/1.1 200 OK
connection: close
content-type: application/json; charset=UTF-8
content-length: 130
server: Java
         */
        byte[] prefix = status.prefix;
        JsonUtil.arraycopy(prefix, 0, output, 0, prefix.length);
        int offset = prefix.length;

        if (contentLength == 0) {
            JsonUtil.arraycopy(CONTENT_LENGTH_O, 0, output, offset, CONTENT_LENGTH_O.length);
            offset += CONTENT_LENGTH_O.length;

            return offset;
        } else {
            JsonUtil.arraycopy(CONTENT_LENGTH_PREFIX, 0, output, offset, CONTENT_LENGTH_PREFIX.length);
            offset += CONTENT_LENGTH_PREFIX.length;

            String r = Integer.toString(contentLength);
            byte[] contentLengthBytes = r.getBytes(StandardCharsets.ISO_8859_1);
            JsonUtil.arraycopy(contentLengthBytes, 0, output, offset, contentLengthBytes.length);
            offset += contentLengthBytes.length;

            JsonUtil.arraycopy(HEADERS_END, 0, output, offset, HEADERS_END.length);
            offset += HEADERS_END.length;

            JsonUtil.arraycopy(content, 0, output, offset, contentLength);
            offset += contentLength;

            return offset;
        }
    }

    public static void sendResponse(NativeNet net, long socket, ByteBuffer buf) {
        int result = net.write(socket, buf, buf.limit());
        if (result == -100) {
            log.warn("can't write would block");
        }
    }

    private static final int[] multipliers = new int[] {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000};
    private static final long[] longMultipliers = new long[] {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000, 10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L, 100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L, 100_000_000_000_000_000L};

    public static int parseInt(byte[] arr, int offset, int len) {
        int value = 0;
        int n = len - 1;
        for(int i=offset;i<offset+len;i++) {
            value += multipliers[n] * (arr[i] - '0');
            n--;
        }
        return value;
    }

    public static long parseLong(byte[] arr, int offset, int len) {
        long value = 0;
        int n = len - 1;
        for(int i=offset;i<offset+len;i++) {
            value += longMultipliers[n] * (arr[i] - '0');
            n--;
        }
        return value;
    }

    public static long parseSignedLong(byte[] arr, int offset, int len) {
        boolean negative;
        if (arr[offset] == '-') {
            negative = true;
            offset++;
            len--;
        } else {
            negative = false;
        }
        long value = 0;
        int n = len - 1;
        for(int i=offset;i<offset+len;i++) {
            value += longMultipliers[n] * (arr[i] - '0');
            n--;
        }
        if (negative) value = value * -1L;
        return value;
    }
}
