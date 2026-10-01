package com.repforge.app;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

final class GoogleRequestPolicy {
    static String networkMessage(Throwable error, String language) {
        boolean network = false;
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnknownHostException || cause instanceof SocketTimeoutException
                    || cause instanceof SocketException) { network = true; break; }
            if (cause.getCause() == cause) break;
        }
        if (!network) return null;
        String[] messages = {
            "A Google nem érhető el hálózati hiba miatt. Ellenőrizd a kapcsolatot, majd próbáld újra. A helyi adatok és a ZIP-mentés továbbra is elérhetők.",
            "Google cannot be reached because of a network error. Check your connection and retry. Local data and ZIP backup remain available.",
            "Google ist wegen eines Netzwerkfehlers nicht erreichbar. Verbindung prüfen und erneut versuchen. Lokale Daten und ZIP-Sicherung bleiben verfügbar.",
            "Google nu poate fi accesat din cauza unei erori de rețea. Verifică conexiunea și reîncearcă. Datele locale și copia ZIP rămân disponibile."
        };
        return messages["hu".equals(language) ? 0 : "de".equals(language) ? 2 : "ro".equals(language) ? 3 : 1];
    }
    static boolean canRetry(String method, String url, String eventId) {
        return method.equals("GET") || method.equals("PUT") || method.equals("DELETE")
                || method.equals("POST") && url.startsWith("https://www.googleapis.com/calendar/v3/calendars/")
                && url.contains("/events?") && eventId.matches("rf[0-9a-f]{64}");
    }
    static boolean transientStatus(int code) {
        return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
    }
    static boolean transientStatus(int code, String reason) {
        return transientStatus(code) || code == 403 && (reason.equals("rateLimitExceeded") || reason.equals("userRateLimitExceeded"));
    }
    static long delay(int attempt, String retryAfter, long now) {
        long delay = 1000L * (1L << attempt);
        if (retryAfter != null) {
            try { delay = Math.max(delay, Long.parseLong(retryAfter) * 1000L); }
            catch (NumberFormatException e) {
                try { delay = Math.max(delay, new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).parse(retryAfter).getTime() - now); }
                catch (Exception ignored) { }
            }
        }
        // A long server cooldown ends this attempt; do not retry before Retry-After.
        return delay > 15000L ? -1 : delay;
    }
}
