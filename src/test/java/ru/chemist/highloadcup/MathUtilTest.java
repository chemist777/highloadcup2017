package ru.chemist.highloadcup;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.chemist.highloadcup.MathUtil.roundUpPowerOfTwo;

public class MathUtilTest {
    @Test
    public void roundUpPowerOfTwoTest() throws Exception {
        assertThat(roundUpPowerOfTwo(1), is(1));
        assertThat(roundUpPowerOfTwo(2), is(2));
        assertThat(roundUpPowerOfTwo(3), is(4));
        assertThat(roundUpPowerOfTwo(4), is(4));
        assertThat(roundUpPowerOfTwo(5), is(8));
        assertThat(roundUpPowerOfTwo(6), is(8));
        assertThat(roundUpPowerOfTwo(9), is(16));
        assertThat(roundUpPowerOfTwo(16), is(16));
        assertThat(roundUpPowerOfTwo(17), is(32));
        assertThat(roundUpPowerOfTwo(1023), is(1024));
        assertThat(roundUpPowerOfTwo(1025), is(2048));
        assertThat(roundUpPowerOfTwo(4095), is(4096));
        assertThat(roundUpPowerOfTwo(4096), is(4096));
    }

}