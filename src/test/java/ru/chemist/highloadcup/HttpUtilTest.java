package ru.chemist.highloadcup;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpUtilTest {
    @Test
    public void tryParse() throws Exception {
        byte[] req = ("POST /visits/2477?query_id=0 HTTP/1.1\r\n" +
                "Host: travels.com\r\n" +
                "User-Agent: tank\r\n" +
                "Accept: */*\r\n" +
                "Connection: Close\r\n" +
                "Content-Length: 11\r\n" +
                "Content-Type: application/json\r\n" +
                "\r\n" +
                "{\"mark\": 3}\r\n" +
                "\r\n").getBytes(StandardCharsets.ISO_8859_1);
        verify(req);
    }

    private void verify(byte[] req) {
        Request request = new Request();
        System.arraycopy(req, 0, request.buf, 0, req.length);
        request.bufSize = req.length;

        HttpUtil.tryParse(request);
        assertThat(request.method, is(HttpMethod.POST));

        FastUriParser.parseUri(request);
        assertThat(request.substring(request.pathStart, request.pathEnd + 1), is("/visits/2477"));

        MutableObject<String> param = new MutableObject<>();
        FastUriParser.parse(request, ((uri, start, eq, end) -> {
            param.setValue(FastUriParser.getValue(uri, eq, end));
        }));

        assertThat(param.getValue(), is("0"));

        String content = new String(request.buf, request.contentOffset, request.contentLen, StandardCharsets.ISO_8859_1);
        assertThat(content, is("{\"mark\": 3}"));
    }

    @Test
    public void tryParse2() throws Exception {
        byte[] req = ("POST /visits/2477?query_id=0 HTTP/1.1\r\n" +
                "Host: travels.com\r\n" +
                "User-Agent: tank\r\n" +
                "Accept: */*\r\n" +
                "Connection: Close\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 11\r\n" +
                "\r\n" +
                "{\"mark\": 3}\r\n" +
                "\r\n").getBytes(StandardCharsets.ISO_8859_1);

        verify(req);
    }

    @Test
    public void parseInt() throws Exception {
        for(int i=0;i<2_000_000;i++) {
            pi(String.valueOf(i));
        }
    }

    private static void pi(String s) {
        assertThat(HttpUtil.parseInt(s.getBytes(), 0, s.length()), is(Integer.parseInt(s)));
    }
}