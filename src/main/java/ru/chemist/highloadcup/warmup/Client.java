package ru.chemist.highloadcup.warmup;

import org.apache.commons.io.IOUtils;
import ru.chemist.highloadcup.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final String ORIGIN = "http://127.0.0.1:" + Server.PORT;

    private URL url(String path) {
        try {
            return new URL(ORIGIN + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String get(String path) {
        try {
            HttpURLConnection con = (HttpURLConnection) url(path).openConnection();
            con.setAllowUserInteraction(false);
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
//            con.setRequestProperty("Connection", "close");

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Bad code: " + responseCode);
            }
            try (InputStream in = con.getInputStream()) {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String post(String path, String data) {
        try {
            byte[] arr = data.getBytes(StandardCharsets.UTF_8);

            HttpURLConnection con = (HttpURLConnection) url(path).openConnection();
            con.setRequestMethod("POST");
            con.setAllowUserInteraction(false);
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);

//            client_8417_1 |22:22:52.111 [Thread-1] INFO ru.chemist.highloadcup.Server - post: POST /visits/2477?query_id=0 HTTP/1.1
//client_8417_1 |Host: travels.com
//client_8417_1 |User-Agent: tank
//client_8417_1 |Accept: */*
//            client_8417_1 |Connection: Close
//            client_8417_1 |Content-Length: 11
//            client_8417_1 |Content-Type: application/json
//            client_8417_1 |client_8417_1 |{"mark": 3}
//

            con.setRequestProperty("Host", "travels.com");
            con.setRequestProperty("User-Agent", "tank");
            con.setRequestProperty("Accept", "*/*");
            con.setRequestProperty("Connection", "Close");
            con.setRequestProperty("Content-Length", Integer.toString(arr.length));
            con.setRequestProperty("Content-Type", "application/json");

            con.setDoOutput(true);

            try(OutputStream out = con.getOutputStream()) {
                out.write(arr);
                out.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Bad code: " + responseCode);
            }
            try (InputStream in = con.getInputStream()) {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
