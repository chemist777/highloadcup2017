package ru.chemist.highloadcup;

import org.junit.Ignore;
import org.junit.Test;
import ru.chemist.highloadcup.warmup.Client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ServerTest {
    @Test
    @Ignore
    public void server() throws Exception {
        Server server = new Server();
        server.start();

        Client client = new Client();

//        String resp = client.get("/locations/34/avg?toDate=1308182400&fromAge=29");
//        String resp = client.get("/locations/455/avg?toAge=58&fromAge=19");
//        System.out.println(resp);

        String resp = client.get("/users/2");
        assertThat(resp, containsString("Теретатина"));

        resp = client.post("/users/2", "{\"first_name\":\"кака\"}");
        assertThat(resp, is("{}"));


        try {
            client.post("/users/777/new", "{\"first_name\":\"кака\"}");
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Bad code: 404"));
        }
        try {
            client.post("/users/new", "{\"id\": 777, \"first_name\": \"кака\"}");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Bad code: 400"));
        }

        resp = client.post("/users/new?query_id=1", "{\"id\": 777, \"first_name\": \"кака\", \"last_name\": \"aa\", \"birth_date\": 3333, \"gender\": \"f\", \"email\": \"kokomail\"}");
        assertThat(resp, is("{}"));
        Thread.sleep(1000);
        resp = client.get("/users/777");
        assertThat(resp, containsString("777"));
        assertThat(resp, containsString("кака"));
        assertThat(resp, containsString("aa"));
        assertThat(resp, containsString("3333"));
        assertThat(resp, containsString("\"f\""));
        assertThat(resp, containsString("kokomail"));

        resp = client.get("/locations/1");
        assertThat(resp, containsString("Египет"));

        resp = client.get("/locations/1/avg");
        assertThat(resp, containsString("2.5"));

        /*
         public final Long id;
    public long location;
    public Long user;
    @JsonProperty("visited_at")
    public long visitedAt;
    public byte mark;
         */
        resp = client.post("/visits/new", "{\"id\": 777, \"location\": 1, \"user\": 777, \"visited_at\": 1344770917, \"mark\": 4}");
        assertThat(resp, is("{}"));
        Thread.sleep(1000);
        resp = client.get("/visits/777");
        assertThat(resp, containsString("777"));
        assertThat(resp, containsString("1344770917"));

        resp = client.get("/locations/1/avg");
        assertThat(resp, containsString("2.52"));

        resp = client.get("/users/777/visits");
        assertThat(resp, containsString("Забор"));

        server.stop();
    }
}
