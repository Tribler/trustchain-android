package nl.tudelft.cs4160.trustchain_android.util;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertEquals;
import static nl.tudelft.cs4160.trustchain_android.util.Util.ellipsize;

/**
 * Created by meijer on 10-11-17.
 */
@RunWith(JUnit4.class)
public class UtilTest {
    String longString;
    String longStringFirst6;
    String shortString;
    String shortStringFirst6;

    @Before
    public void initialization() {
        longString = "0002SDVnnuisdvbhhduifvbsdiuvndskjvcxnvjkdsvusidvbsdjovcnvjkcxv0002SDVnnuisdvbhhduifvbsdiuvndskjvcxnvjkdsvusidvbsdjovcnvjkcxv0002SDVnnuisdvbhhduifvbsdiuvndskjvcxnvjkdsvusidvbsdjovcnvjkcxv";
        longStringFirst6 = "0(..)v";
        shortString = "viudnfvidufdvn84938enfivdnidvn";
        shortStringFirst6 = "v(..)n";
    }

    @Test
    public void ellipsizeTest() throws Exception {
        String input = "12345678910";
        String expected = "1(..)0";
        Assert.assertEquals(expected,ellipsize(input,5));
    }

    @Test
    public void ellipsizeTest2() throws Exception {
        String input = "12345678910";
        String expected = "12(..)10";
        Assert.assertEquals(expected,ellipsize(input,8));
    }

    @Test
    public void ellipsizeTest3() throws Exception {
        String input = "12345678910";
        Assert.assertEquals(input,ellipsize(input,11));
    }

    @Test
    public void ellipsizeTest4() throws Exception {
        String input = "12345678910";
        String expected = "1(..)0";
        Assert.assertEquals(expected,ellipsize(input,6));
    }

    @Test
    public void ellipsizeTest5() throws Exception {
        String input = "12";
        String expected = "12";
        Assert.assertEquals(expected,ellipsize(input,5));
    }
    @Test
    public void testEllipsizeShort() {
        String a = Util.ellipsize(shortString, 6);
        assertEquals(a, shortStringFirst6);
    }

    @Test
    public void testEllipsizeLong() {
        assertEquals(Util.ellipsize(longString, 6), longStringFirst6);
    }

    @Test
    public void testEllipsizeFull() {
        assertEquals(Util.ellipsize(shortString, 1000), shortString);
    }


    @Test
    public void testReadableSize() {
        assertEquals("1 KB", Util.readableSize(1024));
        assertEquals("100 B", Util.readableSize(100));
        assertEquals("1 MB", Util.readableSize(1024*1024));
    }


    @Test
    public void testTimeToString() {
        assertEquals("0s", Util.timeToString(100));
        assertEquals("0s", Util.timeToString(999));
        assertEquals("1s", Util.timeToString(1000));
        assertEquals("2s", Util.timeToString(2000));
        assertEquals("59s", Util.timeToString(1000*60-1));
        assertEquals("1m0s", Util.timeToString(1000*60));
        assertEquals("1m1s", Util.timeToString(1000*60+1000));
        assertEquals("1h0m", Util.timeToString(1000*60*60));
    }
}