package __PACKAGE_NAME__

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.util.UUID

data class StoredPhotoMetadata(
    val id: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
    val createdAt: String
)

/**
 * Edzésfotók fizikai tárolását, tömörítését és kamera/galéria importját végző repository.
 */
class WorkoutPhotoRepository(private val context: Context) {

    private val photosDir = File(context.filesDir, "workout_photos").apply {
        if (!exists()) mkdirs()
    }

    private var pendingCameraCaptureFile: File? = null

    fun photoDir(): File = photosDir

    fun prepareCameraCaptureIntent(): Intent {
        val tempId = UUID.randomUUID().toString()
        val tempFile = File(context.cacheDir, "temp_camera_$tempId.jpg")
        pendingCameraCaptureFile = tempFile

        val authority = "${context.packageName}.fileprovider"
        val photoUri = FileProvider.getUriForFile(context, authority, tempFile)

        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    suspend fun handleCameraResult(resultCode: Int, data: Intent?): Result<StoredPhotoMetadata> = withContext(Dispatchers.IO) {
        if (resultCode != Activity.RESULT_OK) {
            pendingCameraCaptureFile?.delete()
            pendingCameraCaptureFile = null
            return@withContext Result.failure(Exception("Kamera rögzítés megszakítva."))
        }

        val captured = pendingCameraCaptureFile ?: return@withContext Result.failure(Exception("Hiányzó kamera pufferfájl."))
        try {
            val photoId = UUID.randomUUID().toString()
            val targetFile = File(photosDir, "$photoId.jpg")
            val meta = processAndCompressImage(captured.inputStream(), targetFile, photoId)
            captured.delete()
            pendingCameraCaptureFile = null
            Result.success(meta)
        } catch (e: Exception) {
            captured.delete()
            pendingCameraCaptureFile = null
            Result.failure(e)
        }
    }

    suspend fun importPickedUri(uri: Uri?): Result<StoredPhotoMetadata> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext Result.failure(Exception("Nincs kiválasztva kép."))
        try {
            val photoId = UUID.randomUUID().toString()
            val targetFile = File(photosDir, "$photoId.jpg")
            val input = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Nem nyitható meg a képfolyam."))

            val meta = input.use { stream ->
                processAndCompressImage(stream, targetFile, photoId)
            }
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createPickImageIntent(): Intent {
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
    }

    fun delete(id: String): Boolean {
        val file = File(photosDir, "$id.jpg")
        return if (file.exists()) file.delete() else false
    }

    private fun processAndCompressImage(input: InputStream, targetFile: File, id: String): StoredPhotoMetadata {
        val tempRaw = File(context.cacheDir, "raw_$id.tmp")
        FileOutputStream(tempRaw).use { out -> input.copyTo(out) }

        try {
            val exif = ExifInterface(tempRaw.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(tempRaw.absolutePath, options)

            var sampleSize = 1
            val maxDimension = 1920
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decodedBitmap = BitmapFactory.decodeFile(tempRaw.absolutePath, decodeOptions)
                ?: throw IOException("A kép dekódolása sikertelen.")

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val rotatedBitmap = Bitmap.createBitmap(
                decodedBitmap, 0, 0,
                decodedBitmap.width, decodedBitmap.height,
                matrix, true
            )

            FileOutputStream(targetFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            val finalWidth = rotatedBitmap.width
            val finalHeight = rotatedBitmap.height

            if (rotatedBitmap != decodedBitmap) {
                decodedBitmap.recycle()
            }
            rotatedBitmap.recycle()

            return StoredPhotoMetadata(
                id = id,
                width = finalWidth,
                height = finalHeight,
                bytes = targetFile.length(),
                createdAt = Instant.now().toString()
            )
        } finally {
            tempRaw.delete()
        }
    }
}
