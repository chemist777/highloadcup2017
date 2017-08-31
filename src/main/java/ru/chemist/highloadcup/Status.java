package ru.chemist.highloadcup;

import java.nio.charset.StandardCharsets;

public enum Status {
    OK(200, "OK", true), BAD_REQUEST(400, "Bad request", true), NOT_FOUND(404, "Not found", true), INTERNAL_SERVER_ERROR(500, "Internal server error", true),
    OK_NOKEEP(200, "OK", false), BAD_REQUEST_NOKEEP(400, "Bad request", false), NOT_FOUND_NOKEEP(404, "Not found", false), INTERNAL_SERVER_ERROR_NOKEEP(500, "Internal server error", false);

    public final byte[] prefix;

    Status(int code, String text, boolean keepalive) {
        String r = "HTTP/1.1 " + code + " " + text + "\r\n";
        if (keepalive) {
            r += "Connection: keep-alive\r\n"; //it's required by tank
        } else
            r += "Connection: close\r\n";
        r += "Content-Type: application/json\r\n"; //it's required
        prefix = r.getBytes(StandardCharsets.ISO_8859_1);
    }
}
