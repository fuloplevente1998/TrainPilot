package com.repforge.app;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class GoogleRequestPolicy {
    static boolean canRetry(String method, String url, String eventId) {
        return method.equals("GET") || method.equals("PUT") || method.equals("DELETE")
                || method.equals("POST") && url.startsWith("https://www.googleapis.com/calendar/v3/calendars/")
                && url.contains("/events?") && eventId.matches("rf[0-9a-f]{64}");
    }
    static boolean transientStatus(int code) {
        return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
    }
    static long delay(int attempt, String retryAfter, long now) {
        long delay = 1000L * (1L << attempt);
        if (retryAfter != null) {
            try { delay = Math.max(delay, Long.parseLong(retryAfter) * 1000L); }
            catch (NumberFormatException e) {
                try { delay = Math.max(delay, ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - now); }
                catch (Exception ignored) { }
            }
        }
        // A long server cooldown ends this attempt; do not retry before Retry-After.
        return delay > 15000L ? -1 : delay;
    }
}
