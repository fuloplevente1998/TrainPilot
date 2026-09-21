package com.repforge.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.JSONObject;

@CapacitorPlugin(name = "NativeFiles")
public class NativeFilesPlugin extends Plugin {
    private static final int LIMIT = 20 * 1024 * 1024;
    private static final String STATE_PENDING_FILE = "trainpilotPendingExportFile";
    private static final String STATE_PENDING_NAME = "trainpilotPendingExportName";
    private volatile boolean busy = false;
    private File pendingSaveFile = null;
    private String pendingSaveName = null;

    private void cancelled(PluginCall call) {
        if (call == null) return;
        JSObject value = new JSObject();
        value.put("cancelled", true);
        call.resolve(value);
    }

    private File exportTempDir() throws IOException {
        File dir = new File(getContext().getCacheDir(), "backup_export");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Az ideiglenes mentési mappa nem hozható létre.");
        return dir.getCanonicalFile();
    }

    private File createPendingFile(byte[] data) throws IOException {
        File file = File.createTempFile("trainpilot-export-", ".json", exportTempDir());
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(data);
            out.flush();
            out.getFD().sync();
        }
        if (file.length() != data.length || file.length() <= 0) {
            file.delete();
            throw new IOException("Az ideiglenes mentés ellenőrzése sikertelen.");
        }
        return file;
    }

    private byte[] readFile(File file) throws IOException {
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > LIMIT) {
            throw new IOException("Nincs érvényes ideiglenes mentés.");
        }
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > LIMIT) throw new IOException("Maximum 20 MB.");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private synchronized File getPendingSaveFile() { return pendingSaveFile; }

    private synchronized void clearPendingSave(File expected) {
        File file = pendingSaveFile;
        if (expected == null || file == expected || (file != null && expected.equals(file))) {
            pendingSaveFile = null;
            pendingSaveName = null;
        }
        File toDelete = expected != null ? expected : file;
        if (toDelete != null) {
            try { toDelete.delete(); } catch (Exception ignored) { }
        }
    }

    @Override
    protected Bundle saveInstanceState() {
        File file = getPendingSaveFile();
        if (file == null) return super.saveInstanceState();
        Bundle state = new Bundle();
        state.putString(STATE_PENDING_FILE, file.getName());
        state.putString(STATE_PENDING_NAME, pendingSaveName);
        return state;
    }

    @Override
    protected void restoreState(Bundle state) {
        if (state == null) return;
        String token = state.getString(STATE_PENDING_FILE);
        if (token == null || token.isEmpty()) return;
        try {
            File dir = exportTempDir();
            File candidate = new File(dir, token).getCanonicalFile();
            if (!dir.equals(candidate.getParentFile())) return;
            if (!candidate.isFile() || candidate.length() <= 0 || candidate.length() > LIMIT) return;
            synchronized (this) {
                pendingSaveFile = candidate;
                pendingSaveName = state.getString(STATE_PENDING_NAME);
                busy = true;
            }
        } catch (Exception ignored) { }
    }

    @PluginMethod public void save(PluginCall call) {
        String data = call.getString("data");
        if (data == null) { call.reject("Érvénytelen mentés."); return; }
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 0 || bytes.length > LIMIT) { call.reject("Érvénytelen vagy túl nagy mentés."); return; }
        try { new JSONObject(data); } catch (Exception e) { call.reject("Hibás JSON."); return; }
        if (busy) { call.reject("Már nyitva van egy fájlválasztó."); return; }

        String title = call.getString("name", "repforge-backup.json").replaceAll("[^a-zA-Z0-9._-]", "_");
        try {
            File temp = createPendingFile(bytes);
            synchronized (this) {
                pendingSaveFile = temp;
                pendingSaveName = title;
                busy = true;
            }

            // The full backup JSON can be very large. Capacitor also persists PluginCall
            // options into the Activity Bundle, so remove the large payload after staging it.
            // The same callback id remains intact; only the native copy of the call options shrinks.
            call.getData().remove("data");

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, title);
            startActivityForResult(call, intent, "saved");
        } catch (Exception e) {
            busy = false;
            clearPendingSave(null);
            call.reject("Nem nyitható meg a mentési ablak.", e);
        }
    }

    @ActivityCallback private void saved(PluginCall call, ActivityResult result) {
        busy = false;
        File source = getPendingSaveFile();

        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            clearPendingSave(source);
            cancelled(call);
            return;
        }

        Uri uri = result.getData().getData();
        if (uri == null) {
            clearPendingSave(source);
            if (call != null) call.reject("Nincs kiválasztott fájl.");
            return;
        }

        if (source == null) {
            if (call != null) call.reject("Nincs helyreállítható mentési adat.");
            return;
        }

        getBridge().execute(() -> {
            try {
                byte[] expected = readFile(source);
                try (OutputStream out = getContext().getContentResolver().openOutputStream(uri, "w")) {
                    if (out == null) throw new IOException("Nem nyitható meg írásra.");
                    out.write(expected);
                    out.flush();
                }
                byte[] actual = read(uri);
                if (actual.length <= 0 || !Arrays.equals(expected, actual)) {
                    throw new IOException("A visszaolvasott fájl eltér vagy üres.");
                }
                if (call != null) {
                    JSObject value = new JSObject();
                    value.put("cancelled", false);
                    value.put("name", name(uri));
                    value.put("bytes", actual.length);
                    value.put("verified", true);
                    call.resolve(value);
                }
            } catch (Exception e) {
                if (call != null) call.reject("A mentés írása vagy ellenőrzése sikertelen. Próbálj másik mappát.", e);
            } finally {
                clearPendingSave(source);
            }
        });
    }

    @PluginMethod public void open(PluginCall call) {
        if (busy) { call.reject("Már nyitva van egy fájlválasztó."); return; }
        busy = true;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("*/*");
        try { startActivityForResult(call, intent, "opened"); }
        catch (Exception e) { busy = false; call.reject("Nem nyitható meg a fájlválasztó.", e); }
    }

    @ActivityCallback private void opened(PluginCall call, ActivityResult result) {
        busy = false;
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) { cancelled(call); return; }
        Uri uri = result.getData().getData();
        if (uri == null) { call.reject("Nincs kiválasztott fájl."); return; }
        getBridge().execute(() -> {
            try {
                String data = new String(read(uri), StandardCharsets.UTF_8); new JSONObject(data);
                JSObject value = new JSObject(); value.put("data", data); value.put("name", name(uri));
                value.put("cancelled", false); call.resolve(value);
            } catch (Exception e) { call.reject("A fájl nem olvasható, túl nagy vagy nem érvényes JSON.", e); }
        });
    }

    private byte[] read(Uri uri) throws IOException {
        try (InputStream in = getContext().getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IOException("Nem nyitható meg olvasásra.");
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) { if (out.size() + n > LIMIT) throw new IOException("Maximum 20 MB."); out.write(buffer, 0, n); }
            return out.toByteArray();
        }
    }

    private String name(Uri uri) {
        try (Cursor c = getContext().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) { }
        return pendingSaveName == null ? "repforge-backup.json" : pendingSaveName;
    }

    @PluginMethod public void addCalendarEvent(PluginCall call) {
        try {
            long begin=call.getLong("start",0L),end=call.getLong("end",0L);
            if(begin<=0||end<=begin){call.reject("Érvénytelen edzésidőpont.");return;}
            Intent intent=new Intent(Intent.ACTION_INSERT).setData(android.provider.CalendarContract.Events.CONTENT_URI);
            intent.putExtra(android.provider.CalendarContract.Events.TITLE,call.getString("title","RepForge edzés"));
            intent.putExtra(android.provider.CalendarContract.Events.DESCRIPTION,call.getString("description",""));
            intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME,begin);
            intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME,end);
            getActivity().startActivity(intent);JSObject r=new JSObject();r.put("opened",true);call.resolve(r);
        }catch(Exception e){call.reject("Nem nyitható meg naptáralkalmazás.",e);}
    }

    @PluginMethod public void openVideo(PluginCall call) {
        try {
            Uri uri = Uri.parse(call.getString("url", "")); String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || !("www.youtube.com".equals(host) || "www.muscleandstrength.com".equals(host))) { call.reject("Nem támogatott videóhivatkozás."); return; }
            getActivity().startActivity(new Intent(Intent.ACTION_VIEW, uri)); call.resolve();
        } catch (Exception e) { call.reject("Nincs alkalmazás a videó megnyitásához.", e); }
    }
}
