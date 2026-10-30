package com.repforge.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class StoredPhotoMetadata(
    val id: String,
    val file: File,
    val width: Int,
    val height: Int,
    val bytes: Long
)

/**
 * Private workout-progress photos repository (native replacement of WorkoutPhotosPlugin).
 * No broad media/storage permission is requested. Preserves Xiaomi gallery intent fallback,
 * OEM camera EXTRA_OUTPUT / returnedUri / preview Bitmap fallback, and EXIF normalization.
 */
class WorkoutPhotoRepository(context: Context) {
    companion object {
        private const val SOURCE_LIMIT: Long = 40L * 1024L * 1024L
        private const val STORED_MAX_EDGE: Int = 1600
        private const val STORED_QUALITY: Int = 82
        private val PHOTO_ID_REGEX = Regex("[a-f0-9-]{36}")
    }

    private val appContext = context.applicationContext

    @Volatile
    private var cameraTemp: File? = null

    @Volatile
    private var cameraUri: Uri? = null

    fun photoDir(): File {
        val d = File(appContext.filesDir, "workout_photos")
        if (!d.exists() && !d.mkdirs()) throw IOException("A fotómappa nem hozható létre.")
        return d
    }

    fun photoFile(id: String): File {
        if (!PHOTO_ID_REGEX.matches(id)) throw IOException("Hibás fotóazonosító.")
        return File(photoDir(), "$id.jpg")
    }

    fun exists(id: String): Boolean {
        return try {
            photoFile(id).isFile
        } catch (_: Exception) {
            false
        }
    }

    fun delete(id: String): Boolean {
        return try {
            val f = photoFile(id)
            !f.exists() || f.delete()
        } catch (_: Exception) {
            false
        }
    }

    private fun isXiaomiFamily(): Boolean {
        val id = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.US)
        return id.contains("xiaomi") || id.contains("redmi") || id.contains("poco")
    }

    fun createPickImageIntent(): Intent {
        if (isXiaomiFamily()) {
            val xiaomi = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
                setPackage("com.miui.gallery")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (xiaomi.resolveActivity(appContext.packageManager) != null) {
                return xiaomi
            }
        }
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun prepareCameraCaptureIntent(): Intent {
        cleanupCameraTarget(cameraUri, cameraTemp)
        val tempFile = File.createTempFile("trainpilot-camera-", ".jpg", appContext.cacheDir)
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", tempFile)
        cameraTemp = tempFile
        cameraUri = uri

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            putExtra("return-data", false)
            clipData = ClipData.newRawUri("TrainPilot camera", uri)
        }
        val grants = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        intent.addFlags(grants)
        for (ri in appContext.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            val pkg = ri.activityInfo?.packageName
            if (pkg != null) {
                appContext.grantUriPermission(pkg, uri, grants)
            }
        }
        if (intent.resolveActivity(appContext.packageManager) == null) {
            cleanupCameraTarget(uri, tempFile)
            cameraUri = null
            cameraTemp = null
            throw IOException("Nem található kameraalkalmazás.")
        }
        return intent
    }

    suspend fun handleCameraResult(resultCode: Int, data: Intent?): Result<StoredPhotoMetadata?> =
        withContext(Dispatchers.IO) {
            val outputUri = cameraUri
            val outputFile = cameraTemp
            val returnedUri = data?.data
            @Suppress("DEPRECATION")
            val returnedExtra = data?.extras?.get("data")
            cameraUri = null
            cameraTemp = null

            var returnedFile: File? = null
            try {
                // Trust actual written bytes first, even if OEM camera reported RESULT_CANCELED
                if (waitForFileData(outputFile)) {
                    revokeCameraGrant(outputUri)
                    return@withContext Result.success(importSource(null, outputFile))
                }
                if (returnedUri != null && uriHasData(returnedUri)) {
                    try {
                        return@withContext Result.success(importSource(returnedUri, null))
                    } finally {
                        cleanupCameraTarget(outputUri, outputFile)
                    }
                }
                if (returnedExtra is Bitmap) {
                    returnedFile = writeReturnedBitmap(returnedExtra)
                    if (fileHasData(returnedFile)) {
                        cleanupCameraTarget(outputUri, outputFile)
                        val meta = importSource(null, returnedFile)
                        returnedFile = null
                        return@withContext Result.success(meta)
                    }
                }
                cleanupCameraTarget(outputUri, outputFile)
                if (resultCode == android.app.Activity.RESULT_CANCELED) {
                    Result.success(null)
                } else {
                    Result.failure(IOException("A kamera nem adott vissza érvényes képet."))
                }
            } catch (e: Exception) {
                cleanupCameraTarget(outputUri, outputFile)
                returnedFile?.delete()
                Result.failure(e)
            }
        }

    suspend fun importPickedUri(uri: Uri?): Result<StoredPhotoMetadata?> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext Result.success(null)
        try {
            Result.success(importSource(uri, null))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun importSource(uri: Uri?, supplied: File?): StoredPhotoMetadata {
        var temp = supplied
        try {
            if (temp == null) {
                temp = File.createTempFile("trainpilot-import-", ".img", appContext.cacheDir)
                appContext.contentResolver.openInputStream(uri!!).use { input ->
                    if (input == null) throw IOException("A kiválasztott kép nem olvasható.")
                    FileOutputStream(temp).use { out ->
                        val b = ByteArray(16384)
                        var total = 0L
                        while (true) {
                            val n = input.read(b)
                            if (n == -1) break
                            total += n
                            if (total > SOURCE_LIMIT) throw IOException("A kiválasztott kép túl nagy.")
                            out.write(b, 0, n)
                        }
                    }
                }
            }
            val id = UUID.randomUUID().toString().lowercase(Locale.ROOT)
            val dest = photoFile(id)
            val (width, height) = normalize(temp!!, dest, STORED_MAX_EDGE, STORED_QUALITY)
            return StoredPhotoMetadata(id, dest, width, height, dest.length())
        } finally {
            temp?.delete()
        }
    }

    private fun normalize(src: File, dest: File, maxEdge: Int, quality: Int): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Nem támogatott vagy sérült kép.")
        }
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxEdge * 2) {
            sample *= 2
        }
        val opt = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = BitmapFactory.decodeFile(src.absolutePath, opt)
            ?: throw IOException("A kép nem dekódolható.")
        try {
            var rotation = 0
            try {
                val exif = ExifInterface(src.absolutePath)
                rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (_: Exception) {
            }
            if (rotation != 0) {
                val m = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                if (rotated !== bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }
            val w = bitmap.width
            val h = bitmap.height
            val scale = min(1.0, maxEdge.toDouble() / max(w, h).toDouble())
            if (scale < 1.0) {
                val nw = max(1, (w * scale).roundToInt())
                val nh = max(1, (h * scale).roundToInt())
                val scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
                if (scaled !== bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }
            FileOutputStream(dest).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                    throw IOException("A JPEG mentése nem sikerült.")
                }
            }
            return bitmap.width to bitmap.height
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun uriHasData(uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { it.read() != -1 } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun fileHasData(file: File?): Boolean {
        return file != null && file.isFile && file.length() > 0L
    }

    private suspend fun waitForFileData(file: File?): Boolean {
        for (attempt in 0 until 5) {
            if (fileHasData(file)) return true
            if (attempt < 4) delay(50L)
        }
        return false
    }

    private fun writeReturnedBitmap(bitmap: Bitmap): File {
        val file = File.createTempFile("trainpilot-camera-return-", ".jpg", appContext.cacheDir)
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                throw IOException("A kamera előnézeti képének mentése sikertelen.")
            }
        }
        return file
    }

    private fun cleanupCameraTarget(uri: Uri?, file: File?) {
        revokeCameraGrant(uri)
        try {
            file?.delete()
        } catch (_: Exception) {
        }
    }

    private fun revokeCameraGrant(uri: Uri?) {
        if (uri == null) return
        try {
            appContext.revokeUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
    }
}
