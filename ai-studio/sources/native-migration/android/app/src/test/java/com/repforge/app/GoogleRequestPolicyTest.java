package com.repforge.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class GoogleRequestPolicyTest {
    @Test public void networkFailuresAreReadableAndDistinctFromDataErrors() {
        String hu=GoogleRequestPolicy.networkMessage(new java.net.UnknownHostException("www.googleapis.com"),"hu");
        assertTrue(hu.contains("hálózati hiba"));assertTrue(hu.contains("ZIP"));assertFalse(hu.contains("googleapis.com"));
        assertTrue(GoogleRequestPolicy.networkMessage(new java.net.SocketTimeoutException("timeout"),"en").contains("network error"));
        assertTrue(GoogleRequestPolicy.networkMessage(new java.net.ConnectException("refused"),"de").contains("Netzwerkfehler"));
        assertTrue(GoogleRequestPolicy.networkMessage(new java.io.IOException(new java.net.UnknownHostException()),"ro").contains("rețea"));
        assertNull(GoogleRequestPolicy.networkMessage(new java.io.IOException("Missing photo"),"hu"));
        assertNull(GoogleRequestPolicy.networkMessage(new java.io.FileNotFoundException("photo.jpg"),"hu"));
    }
    @Test public void retriesOnlyIdempotentOperations() {
        assertTrue(GoogleRequestPolicy.canRetry("GET", "https://www.googleapis.com/drive/v3/files", ""));
        assertTrue(GoogleRequestPolicy.canRetry("DELETE", "https://www.googleapis.com/drive/v3/files/id", ""));
        assertFalse(GoogleRequestPolicy.canRetry("POST", "https://www.googleapis.com/upload/drive/v3/files", ""));
        assertFalse(GoogleRequestPolicy.canRetry("POST", "https://www.googleapis.com/calendar/v3/calendars", ""));
        String url="https://www.googleapis.com/calendar/v3/calendars/id/events?sendUpdates=none";
        assertTrue(GoogleRequestPolicy.canRetry("POST",url,"rf"+new String(new char[64]).replace('\0','a')));
        assertFalse(GoogleRequestPolicy.canRetry("POST",url,"random-id"));
    }
    @Test public void respectsCooldownAndBoundsRetries() {
        assertEquals(1000L,GoogleRequestPolicy.delay(0,null,0));
        assertEquals(4000L,GoogleRequestPolicy.delay(2,"1",0));
        assertEquals(7000L,GoogleRequestPolicy.delay(0,"7",0));
        assertEquals(-1L,GoogleRequestPolicy.delay(0,"60",0));
        assertTrue(GoogleRequestPolicy.transientStatus(429));
        assertFalse(GoogleRequestPolicy.transientStatus(401));
        assertFalse(GoogleRequestPolicy.transientStatus(403));
        assertTrue(GoogleRequestPolicy.transientStatus(403,"userRateLimitExceeded"));
        assertFalse(GoogleRequestPolicy.transientStatus(403,"insufficientPermissions"));
        assertEquals(7000L,GoogleRequestPolicy.delay(0,"Thu, 01 Jan 1970 00:00:07 GMT",0));
    }
}
