package ru.chemist.highloadcup;

import java.nio.charset.StandardCharsets;

public class FastUriParser {
    public static void parse(Request request, Callback callback) {
        int start = request.pathEnd + 2;
        int eq = -1;
        int end = -1;
        for(int i=start; i <= request.uriEnd;i++) {
            byte c = request.buf[i];
            switch (c) {
                case '=':
                    eq = i;
                    break;
                case '&':
                    end = i - 1;
                    callback.accept(request.buf, start, eq, end);
                    start = i + 1;
                    eq = -1;
                    end = -1;
                    break;
            }
        }
        if (start <= request.uriEnd) {
//            if (end < 0) {
            end = request.uriEnd;
//            }
            callback.accept(request.buf, start, eq, end);
        }
    }

    public static void parseUri(Request request) {
        for(int i=request.uriStart;i <= request.uriEnd;i++) {
            if (request.buf[i] == '?') {
                request.pathStart = request.uriStart;
                request.pathEnd = i - 1;
                return;
            }
        }
        request.pathStart = request.uriStart;
        request.pathEnd = request.uriEnd;
    }

    public static boolean isParam(byte[] uri, int start, byte[] name) {
        int uriLen = uri.length;
        for(int i=0;i<name.length;i++) {
            if (i == uriLen || uri[start] != name[i]) return false;
            start++;
        }
        return true;
    }

    //for tests only!
    public static String getValue(byte[] uri, int eq, int end) {
        if (eq < 0 || eq == end) return "";
        return new String(uri, eq + 1, end - eq);
    }

    public static String getStringValue(byte[] uri, int eq, int end) {
        if (eq < 0 || eq == end) return "";
        return decodeComponent(uri, eq + 1, end);
    }

    public static String decodeComponent(final byte[] uri, final int start, final int end) {
        boolean modified = false;
        for (int i = start; i <= end; i++) {
            final byte c = uri[i];
            if (c == '%' || c == '+') {
                modified = true;
                break;
            }
        }
        if (!modified) {
            return new String(uri, start, end - start + 1);
        }
        final int size = end - start + 1;
        final byte[] buf = new byte[size];
        int pos = 0;  // position in `buf'.
        for (int i = start; i <= end; i++) {
            byte c = uri[i];
            switch (c) {
                case '+':
                    buf[pos++] = ' ';  // "+" -> " "
                    break;
                case '%':
                    if (i == end) {
                        throw new IllegalArgumentException("unterminated escape"
                                + " sequence at end of string: " + new String(uri));
                    }
                    c = uri[++i];
                    if (c == '%') {
                        buf[pos++] = '%';  // "%%" -> "%"
                        break;
                    }
                    if (i == end) {
                        throw new IllegalArgumentException("partial escape"
                                + " sequence at end of string: " + new String(uri));
                    }
                    char c1 = decodeHexNibble(c);
                    final char c2 = decodeHexNibble(uri[++i]);
                    if (c1 == Character.MAX_VALUE || c2 == Character.MAX_VALUE) {
                        throw new IllegalArgumentException(
                                "invalid escape sequence `%" + uri[i - 1]
                                        + uri[i] + "' at index " + (i - 2)
                                        + " of: " + new String(uri));
                    }
                    c = (byte) (char) (c1 * 16 + c2);
                    // Fall through.
                default:
                    buf[pos++] = c;
                    break;
            }
        }
        return new String(buf, 0, pos, StandardCharsets.UTF_8);
    }

    /**
     * Helper to decode half of a hexadecimal number from a string.
     * @param c The ASCII character of the hexadecimal number to decode.
     * Must be in the range {@code [0-9a-fA-F]}.
     * @return The hexadecimal value represented in the ASCII character
     * given, or {@link Character#MAX_VALUE} if the character is invalid.
     */
    private static char decodeHexNibble(final byte c) {
        if ('0' <= c && c <= '9') {
            return (char) (c - '0');
        } else if ('a' <= c && c <= 'f') {
            return (char) (c - 'a' + 10);
        } else if ('A' <= c && c <= 'F') {
            return (char) (c - 'A' + 10);
        } else {
            return Character.MAX_VALUE;
        }
    }

    @FunctionalInterface
    public interface Callback {
        void accept(byte[] uri, int start, int eq, int end);
    }
}
