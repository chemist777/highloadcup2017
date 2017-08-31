package ru.chemist.highloadcup;

import org.junit.Test;

import java.time.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RequestHandlerTest {
    @Test
    public void avg() throws Exception {
        double total = 2453125;
        int count = 1000000;
        double result = total / count;
        System.out.println(result);


        result = (double) Math.round(result * 100000d) / 100000d;

//        DecimalFormat df = new DecimalFormat("0.#####");
//        df.setRoundingMode(RoundingMode.HALF_UP);

        assertThat(String.valueOf(result), is("2.45313"));
    }

    @Test
    public void ageCalc() {
        ZoneId zoneId = ZoneId.of("UTC");
        Clock clock = Clock.system(zoneId);

        for(int fromAge = 0; fromAge < 1000; fromAge ++) {
            Instant now = Instant.now();
            long javaTime = now.atZone(zoneId).toLocalDateTime().minusYears(fromAge).toEpochSecond(ZoneOffset.UTC);
            long myTime = LocalDateTime.now(clock).minusYears(fromAge).toEpochSecond(ZoneOffset.UTC);
            assertThat("fromAge " + fromAge, myTime, is(javaTime));
        }
    }
}