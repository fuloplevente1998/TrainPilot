package com.repforge.migrationseed;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** CI-only seed for the actual, unchanged old release. No R8-mapped app API. */
public final class LegacySeedInstrumentation extends Instrumentation {
    private boolean verifyMode;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        verifyMode = arguments != null && "verify".equals(arguments.getString("mode"));
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            if (verifyMode) verifyNative(); else seed();
            result.putString(verifyMode ? "verifyStatus" : "seedStatus", "ok");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            Log.e("TrainPilotMigrationTest", "Upgrade test failed", error);
            result.putString(verifyMode ? "verifyStatus" : "seedStatus", "failed");
            result.putString("error", error.toString());
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private static WebView findWebView(View view) {
        if (view instanceof WebView) return (WebView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private String evaluate(WebView view, String script) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        String[] value = new String[1];
        runOnMainSync(() -> view.evaluateJavascript(script, result -> { value[0] = result; done.countDown(); }));
        if (!done.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("JavaScript callback timed out");
        return value[0];
    }

    private void awaitTrue(WebView view, String expression) throws Exception {
        long deadline = System.currentTimeMillis() + 60000;
        while (!"true".equals(evaluate(view, expression))) {
            if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Timed out: " + expression);
            Thread.sleep(100);
        }
    }

    private static String digest(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }

    private static void write(File file, String value) throws Exception {
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject fixture() throws Exception {
        try (java.io.InputStream stream = getContext().getAssets().open("legacy-v1371.json")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new JSONObject(bytes.toString("UTF-8"));
        }
    }

    private void verifyNative() throws Exception {
        Context target = getTargetContext();
        Activity activity = startActivitySync(new Intent().setClassName("com.repforge.app", "com.repforge.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            Class<?> probe = target.getClassLoader().loadClass("com.repforge.app.NativeMigrationProbe");
            probe.getMethod("verify", Context.class, String.class).invoke(null, target, fixture().toString());
        } catch (java.lang.reflect.InvocationTargetException wrapped) {
            throw new IllegalStateException("Native data verification failed", wrapped.getCause());
        } finally { runOnMainSync(activity::finish); }
    }

    private void seed() throws Exception {
        Context target = getTargetContext();
        JSONObject fixture = fixture();
        Activity activity = startActivitySync(new Intent().setClassName("com.repforge.app", "com.repforge.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            WebView[] found = new WebView[1];
            runOnMainSync(() -> found[0] = findWebView(activity.getWindow().getDecorView()));
            WebView web = found[0];
            if (web == null) throw new IllegalStateException("Original release has no WebView");
            awaitTrue(web, "Boolean(window.TrainPilotBoot?.finished)");
            String source = fixture.getJSONObject("storage").toString();
            String script = "(()=>{const values=" + source + ";localStorage.setItem('repforge:history','[]');localStorage.setItem('repforge:obsolete','deleted');localStorage.removeItem('repforge:obsolete');for(const [k,v] of Object.entries(values))localStorage.setItem(k,v);return true;})()";
            if (!"true".equals(evaluate(web, script))) throw new IllegalStateException("Fixture write failed");
            write(new File(target.getNoBackupFilesDir(), "pre-native-fixture-backup.json"), source);
            write(new File(target.getNoBackupFilesDir(), "pre-native-fixture-backup.sha256"), digest(source));
            File gps = new File(target.getNoBackupFilesDir(), "gps_routes");
            if (!gps.isDirectory() && !gps.mkdirs()) throw new IllegalStateException("GPS fixture directory unavailable");
            write(new File(gps, digest(fixture.getString("routeKey")) + ".route"), fixture.getString("routeText"));
            File photos = new File(target.getFilesDir(), "workout_photos");
            if (!photos.isDirectory() && !photos.mkdirs()) throw new IllegalStateException("Photo fixture directory unavailable");
            try (java.io.OutputStream stream = Files.newOutputStream(new File(photos, fixture.getString("photoId") + ".jpg").toPath())) {
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 85, stream);
            }
            evaluate(web, "window.__seedHealthReady=false;(async()=>{try{const p=Capacitor.Plugins.HealthJournal||Capacitor.registerPlugin('HealthJournal');await p.initialize();window.__seedHealthReady=true;}catch(e){window.__seedHealthError=String(e);}})();true");
            awaitTrue(web, "window.__seedHealthReady===true");
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(target.getDatabasePath("health-journal.db").getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
                db.execSQL("INSERT OR REPLACE INTO meta(key,value) VALUES('migration-fixture','retained-health-data')");
            }
            Thread.sleep(1000); // Allow Chromium to commit before package replacement.
        } finally { runOnMainSync(activity::finish); }
    }
}
