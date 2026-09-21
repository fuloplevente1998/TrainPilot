package com.repforge.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.core.content.FileProvider;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.io.*;
import java.util.UUID;
import java.util.Locale;

/** Private workout-progress photos. No broad media/storage permission is requested. */
@CapacitorPlugin(name = "WorkoutPhotos", requestCodes = {8041, 8042})
public class WorkoutPhotosPlugin extends Plugin {
    private static final long SOURCE_LIMIT = 40L * 1024L * 1024L;
    private static final int STORED_MAX_EDGE = 1600;
    private static final int STORED_QUALITY = 82;
    private static final int REQUEST_PICK = 8041;
    private static final int REQUEST_CAPTURE = 8042;
    private volatile boolean busy = false;
    private volatile File cameraTemp;
    private volatile Uri cameraUri;
    private volatile boolean cameraUsesMediaStore = false;
    private volatile PluginCall pendingPhotoCall;
    private volatile int pendingRequestCode = -1;

    private File photoDir() throws IOException {
        File d = new File(getContext().getFilesDir(), "workout_photos");
        if (!d.exists() && !d.mkdirs()) throw new IOException("A fotómappa nem hozható létre.");
        return d;
    }
    private String cleanId(String id) throws IOException {
        if (id == null || !id.matches("[a-f0-9-]{36}")) throw new IOException("Hibás fotóazonosító.");
        return id;
    }
    private File photoFile(String id) throws IOException { return new File(photoDir(), cleanId(id) + ".jpg"); }

    private boolean isXiaomiFamily() {
        String id = (Build.MANUFACTURER + " " + Build.BRAND).toLowerCase(Locale.US);
        return id.contains("xiaomi") || id.contains("redmi") || id.contains("poco");
    }

    private Intent xiaomiGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.setPackage("com.miui.gallery");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    private Intent localOnlyImageIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    @SuppressWarnings("deprecation")
    @PluginMethod public void pick(PluginCall call) {
        if (busy) { call.reject("Már folyamatban van egy fotóművelet."); return; }
        busy = true;
        pendingPhotoCall = call;
        pendingRequestCode = REQUEST_PICK;
        getBridge().executeOnMainThread(() -> {
            try {
                if (isXiaomiFamily()) {
                    try {
                        getActivity().startActivityForResult(xiaomiGalleryIntent(), REQUEST_PICK);
                        return;
                    } catch (android.content.ActivityNotFoundException ignored) {
                        // Fall through to the manufacturer-neutral local-only picker.
                    }
                }
                getActivity().startActivityForResult(localOnlyImageIntent(), REQUEST_PICK);
            } catch (Exception e) {
                pendingPhotoCall = null; pendingRequestCode = -1; busy = false;
                call.reject("Nem nyitható meg a helyi képválasztó.", e);
            }
        });
    }

    private Uri createMediaStoreCameraUri() throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "trainpilot-capture-" + UUID.randomUUID() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TrainPilotTemp");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("A kamera ideiglenes MediaStore képe nem hozható létre.");
        return uri;
    }

    private boolean uriHasData(Uri uri) {
        if (uri == null) return false;
        try (InputStream in = getContext().getContentResolver().openInputStream(uri)) {
            return in != null && in.read() != -1;
        } catch (Exception ignored) { return false; }
    }

    private void deleteCameraMediaStoreUri(Uri uri) {
        if (uri == null) return;
        try { getContext().getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
    }

    private void revokeCameraGrant(Uri uri) {
        if (uri == null) return;
        try {
            getContext().revokeUriPermission(uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("deprecation")
    @PluginMethod public void capture(PluginCall call) {
        if (busy) { call.reject("Már folyamatban van egy fotóművelet."); return; }
        busy = true;
        pendingPhotoCall = call;
        pendingRequestCode = REQUEST_CAPTURE;
        getBridge().executeOnMainThread(() -> {
            try {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraTemp != null) cameraTemp.delete();
                cameraTemp = null; cameraUri = null; cameraUsesMediaStore = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cameraUri = createMediaStoreCameraUri();
                    cameraUsesMediaStore = true;
                } else {
                    cameraTemp = File.createTempFile("trainpilot-camera-", ".jpg", getContext().getCacheDir());
                    cameraUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", cameraTemp);
                }

                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                intent.putExtra("return-data", false);
                intent.setClipData(ClipData.newRawUri("TrainPilot photo", cameraUri));
                int grants = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;
                intent.addFlags(grants);
                try {
                    for (android.content.pm.ResolveInfo ri : getContext().getPackageManager().queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)) {
                        if (ri.activityInfo != null) getContext().grantUriPermission(ri.activityInfo.packageName, cameraUri, grants);
                    }
                } catch (Exception ignored) {}
                getActivity().startActivityForResult(intent, REQUEST_CAPTURE);
            } catch (android.content.ActivityNotFoundException e) {
                revokeCameraGrant(cameraUri);
                if (cameraUsesMediaStore) deleteCameraMediaStoreUri(cameraUri);
                if (cameraTemp != null) cameraTemp.delete();
                cameraUri = null; cameraTemp = null; cameraUsesMediaStore = false;
                pendingPhotoCall = null; pendingRequestCode = -1; busy = false;
                call.reject("Nem található kameraalkalmazás.", e);
            } catch (Exception e) {
                revokeCameraGrant(cameraUri);
                if (cameraUsesMediaStore) deleteCameraMediaStoreUri(cameraUri);
                if (cameraTemp != null) cameraTemp.delete();
                cameraUri = null; cameraTemp = null; cameraUsesMediaStore = false;
                pendingPhotoCall = null; pendingRequestCode = -1; busy = false;
                call.reject("A kamera nem indítható.", e);
            }
        });
    }

    private void importCapturedMediaStore(PluginCall call, Uri uri) {
        try { importUri(call, uri, null); }
        finally {
            revokeCameraGrant(uri);
            deleteCameraMediaStoreUri(uri);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK && requestCode != REQUEST_CAPTURE) return;
        PluginCall call = pendingPhotoCall;
        if (pendingRequestCode != requestCode) return;
        pendingPhotoCall = null;
        pendingRequestCode = -1;
        busy = false;

        if (requestCode == REQUEST_PICK) {
            if (call == null) return;
            Uri uri = data == null ? null : data.getData();
            if (uri == null) {
                JSObject r = new JSObject(); r.put("cancelled", true); call.resolve(r); return;
            }
            getBridge().execute(() -> importUri(call, uri, null));
            return;
        }

        Uri outputUri = cameraUri;
        File outputFile = cameraTemp;
        boolean mediaStore = cameraUsesMediaStore;
        cameraUri = null; cameraTemp = null; cameraUsesMediaStore = false;

        boolean hasImage = mediaStore ? uriHasData(outputUri) : (outputFile != null && outputFile.isFile() && outputFile.length() > 0);
        // Accept a non-empty output even if an OEM camera reports RESULT_CANCELED.
        if (call == null || !hasImage) {
            revokeCameraGrant(outputUri);
            if (mediaStore) deleteCameraMediaStoreUri(outputUri);
            if (outputFile != null) outputFile.delete();
            if (call != null) { JSObject r = new JSObject(); r.put("cancelled", true); call.resolve(r); }
            return;
        }

        if (mediaStore) {
            Uri captured = outputUri;
            getBridge().execute(() -> importCapturedMediaStore(call, captured));
        } else {
            revokeCameraGrant(outputUri);
            File captured = outputFile;
            getBridge().execute(() -> importUri(call, null, captured));
        }
    }

    private void importUri(PluginCall call, Uri uri, File supplied) {
        File temp = supplied;
        try {
            if (temp == null) {
                temp = File.createTempFile("trainpilot-import-", ".img", getContext().getCacheDir());
                try (InputStream in = getContext().getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(temp)) {
                    if (in == null) throw new IOException("A kiválasztott kép nem olvasható.");
                    byte[] b = new byte[16384]; int n; long total = 0;
                    while ((n = in.read(b)) != -1) { total += n; if (total > SOURCE_LIMIT) throw new IOException("A kiválasztott kép túl nagy."); out.write(b, 0, n); }
                }
            }
            String id = UUID.randomUUID().toString().toLowerCase();
            File dest = photoFile(id);
            ImageInfo info = normalize(temp, dest, STORED_MAX_EDGE, STORED_QUALITY);
            JSObject r = new JSObject(); r.put("cancelled", false); r.put("id", id); r.put("mimeType", "image/jpeg");
            r.put("width", info.width); r.put("height", info.height); r.put("bytes", dest.length());
            call.resolve(r);
        } catch (Exception e) { call.reject(e.getMessage() == null ? "A fotó mentése nem sikerült." : e.getMessage(), e); }
        finally { if (temp != null) temp.delete(); }
    }

    private static class ImageInfo { final int width, height; ImageInfo(int w, int h){width=w;height=h;} }
    private ImageInfo normalize(File src, File dest, int maxEdge, int quality) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true; BitmapFactory.decodeFile(src.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Nem támogatott vagy sérült kép.");
        int sample = 1; while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > maxEdge * 2) sample *= 2;
        BitmapFactory.Options opt = new BitmapFactory.Options(); opt.inSampleSize = sample; Bitmap bitmap = BitmapFactory.decodeFile(src.getAbsolutePath(), opt);
        if (bitmap == null) throw new IOException("A kép nem dekódolható.");
        try {
            int rotation = 0;
            try {
                ExifInterface exif = new ExifInterface(src.getAbsolutePath());
                int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90; else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180; else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            } catch (Exception ignored) {}
            if (rotation != 0) { Matrix m = new Matrix(); m.postRotate(rotation); Bitmap r = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true); if (r != bitmap) { bitmap.recycle(); bitmap = r; } }
            int w = bitmap.getWidth(), h = bitmap.getHeight(); double scale = Math.min(1d, (double)maxEdge / Math.max(w, h));
            if (scale < 1d) { int nw = Math.max(1, (int)Math.round(w * scale)), nh = Math.max(1, (int)Math.round(h * scale)); Bitmap s = Bitmap.createScaledBitmap(bitmap, nw, nh, true); if (s != bitmap) { bitmap.recycle(); bitmap = s; } }
            try (OutputStream out = new FileOutputStream(dest)) { if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) throw new IOException("A JPEG mentése nem sikerült."); }
            return new ImageInfo(bitmap.getWidth(), bitmap.getHeight());
        } finally { if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle(); }
    }

    @PluginMethod public void storeDataUrl(PluginCall call) {
        final String dataUrl = call.getString("dataUrl", "");
        getBridge().execute(() -> {
            File temp = null;
            try {
                int comma = dataUrl.indexOf(',');
                if (comma <= 0) throw new IOException("A kamera nem adott vissza érvényes képet.");
                String header = dataUrl.substring(0, comma).toLowerCase();
                if (!(header.equals("data:image/jpeg;base64") || header.equals("data:image/jpg;base64") || header.equals("data:image/png;base64") || header.equals("data:image/webp;base64")))
                    throw new IOException("Nem támogatott képformátum.");
                String encoded = dataUrl.substring(comma + 1);
                if ((long) encoded.length() > SOURCE_LIMIT * 2L) throw new IOException("A kiválasztott kép túl nagy.");
                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                if (bytes.length == 0 || bytes.length > SOURCE_LIMIT) throw new IOException("A kiválasztott kép túl nagy vagy üres.");
                temp = File.createTempFile("trainpilot-camera-plugin-", ".img", getContext().getCacheDir());
                try (OutputStream out = new FileOutputStream(temp)) { out.write(bytes); }
                File supplied = temp; temp = null;
                importUri(call, null, supplied);
            } catch (Exception e) {
                call.reject(e.getMessage() == null ? "A fotó mentése nem sikerült." : e.getMessage(), e);
            } finally {
                if (temp != null) temp.delete();
            }
        });
    }

    @PluginMethod public void read(PluginCall call) {
        getBridge().execute(() -> {
            try {
                File f = photoFile(call.getString("id", "")); if (!f.isFile()) throw new FileNotFoundException("A fotó nincs ezen a készüléken.");
                int maxPx = Math.max(160, Math.min(1600, call.getInt("maxPx", 720)));
                File temp = File.createTempFile("trainpilot-preview-", ".jpg", getContext().getCacheDir());
                try {
                    ImageInfo info = normalize(f, temp, maxPx, 78);
                    byte[] bytes = readLimited(temp, 6 * 1024 * 1024);
                    JSObject r = new JSObject(); r.put("dataUrl", "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)); r.put("width", info.width); r.put("height", info.height); call.resolve(r);
                } finally { temp.delete(); }
            } catch (Exception e) { call.reject(e.getMessage() == null ? "A fotó nem olvasható." : e.getMessage()); }
        });
    }

    @PluginMethod public void exists(PluginCall call) {
        try { JSObject r = new JSObject(); r.put("exists", photoFile(call.getString("id", "")).isFile()); call.resolve(r); }
        catch (Exception e) { call.reject(e.getMessage()); }
    }

    @PluginMethod public void delete(PluginCall call) {
        try { File f = photoFile(call.getString("id", "")); JSObject r = new JSObject(); r.put("deleted", !f.exists() || f.delete()); call.resolve(r); }
        catch (Exception e) { call.reject(e.getMessage()); }
    }

    private byte[] readLimited(File f, int limit) throws IOException {
        try (InputStream in = new FileInputStream(f); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) { if (out.size() + n > limit) throw new IOException("A fotó túl nagy."); out.write(b, 0, n); } return out.toByteArray();
        }
    }
}
