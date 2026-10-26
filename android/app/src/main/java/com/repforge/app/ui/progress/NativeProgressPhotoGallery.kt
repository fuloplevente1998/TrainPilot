package com.repforge.app.ui.progress

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repforge.app.R
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Canonical DB photo ID + its real owning workout, not a guessed file timestamp. */
data class NativeProgressPhoto(
    val id: String,
    val label: String,
    val workoutStartedEpochMs: Long,
    val programName: String,
    val dayName: String,
    val originDevice: String
)

/** Read-only, metadata-first projection. Never accepts private-dir contents as an index. */
internal object NativeProgressPhotoData {
    private const val PAGE = 256
    private val privatePhotoId = Regex("[a-f0-9-]{36}")

    fun load(db: TrainPilotDatabase): List<NativeProgressPhoto> {
        val photos = ArrayList<NativeProgressPhoto>()
        var offset = 0
        var count: Int
        do {
            count = 0
            // Project five scalars, not @Relation, rawJson, route or photo bytes.
            // The matching active/deleted semantics are the same as the native
            // cloud-photo reference check. A physically missing file stays as a
            // missing-photo state, never silently becomes a different image.
            val sql = """
                SELECT p.id, p.label, w.startedEpochMs, w.programName, w.dayName, w.originDevice
                FROM workout_photos p
                JOIN workout_history w ON w.id = p.workoutHistoryId
                WHERE (p.deletedAt IS NULL OR TRIM(p.deletedAt) IN ('', '0', '0.0', 'false'))
                  AND w.finishedEpochMs > w.startedEpochMs
                  AND NOT EXISTS (SELECT 1 FROM history_tombstones t
                    WHERE t.primaryId IN (w.id, w.syncId, w.workoutKey)
                       OR t.workoutKey = w.workoutKey)
                ORDER BY w.startedEpochMs DESC, w.id DESC, p.id DESC
                LIMIT ${PAGE} OFFSET ${offset}
            """.trimIndent()
            db.openHelper.readableDatabase.query(sql).use { rows ->
                while (rows.moveToNext()) {
                    count++
                    val id = rows.getString(0)
                    if (!privatePhotoId.matches(id)) continue
                    photos.add(NativeProgressPhoto(
                        id = id,
                        label = rows.getString(1).orEmpty(),
                        workoutStartedEpochMs = rows.getLong(2),
                        programName = rows.getString(3).orEmpty(),
                        dayName = rows.getString(4).orEmpty(),
                        originDevice = rows.getString(5).orEmpty()
                    ))
                }
            }
            offset += count
        } while (count == PAGE)
        return photos
    }
}

internal object NativeProgressPhotoDecode {
    /**
     * A single requested preview only, always off the UI thread.
     * A thumbnail uses <= 320px/edge; detail uses <= 1600px/edge.
     * Reject malformed dimensions and oversized/non-private inputs.
     */
    fun decode(file: File, requestedEdge: Int): ImageBitmap? {
        val edge = requestedEdge.coerceIn(1, 1600)
        if (!file.isFile || file.length() <= 3L || file.length() > 40L * 1024L * 1024L) return null
        // Robolectric may return a synthetic bitmap for arbitrary invalid bytes;
        // the private repository contract always stores normalized JPEG files.
        // Verify the JPEG SOI marker before asking the platform decoder.
        val jpeg = file.inputStream().use { input ->
            input.read() == 0xff && input.read() == 0xd8 && input.read() == 0xff
        }
        if (!jpeg) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > edge && sample < 1_073_741_824)
            sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        })?.asImageBitmap()
    }
}

private sealed interface PhotoPreview {
    data object Loading : PhotoPreview
    data class Ready(val image: ImageBitmap) : PhotoPreview
    data object Error : PhotoPreview
}

@Composable
private fun PrivateProgressPhoto(id: String, edge: Int, language: String, modifier: Modifier) {
    val context = LocalContext.current
    val colors = LocalTrainPilotColors.current
    val preview by produceState<PhotoPreview>(PhotoPreview.Loading, id, edge) {
        value = withContext(Dispatchers.IO) {
            try {
                val privateFile = WorkoutPhotoRepository(context).photoFile(id)
                NativeProgressPhotoDecode.decode(privateFile, edge)?.let(PhotoPreview::Ready)
                    ?: PhotoPreview.Error
            } catch (_: Exception) {
                PhotoPreview.Error
            }
        }
    }
    val strings = remember(context, language) { nativeResources(context, language) }
    Box(modifier, contentAlignment = Alignment.Center) {
        when (val result = preview) {
            PhotoPreview.Loading -> Text(strings.getString(R.string.native_progress_photo_loading), color = colors.muted)
            PhotoPreview.Error -> Text(strings.getString(R.string.native_progress_photo_unavailable),
                color = colors.text, modifier = Modifier.testTag("progress_photo_error"))
            is PhotoPreview.Ready -> Image(result.image,
                strings.getString(R.string.native_progress_photo_title),
                Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
internal fun NativeProgressPhotoGallery(
    state: ProgressUiState,
    open: (String) -> Unit,
    close: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val locale = remember(state.language) { Locale.forLanguageTag(state.language) }
    val date = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .withZone(ZoneId.systemDefault())
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("progress_photo_gallery"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.getString(R.string.native_progress_photo_title), color = colors.text)
            if (state.photosLoading) {
                Text(strings.getString(R.string.native_progress_photo_loading), color = colors.muted)
            } else if (state.photosError) {
                Text(strings.getString(R.string.native_progress_photo_unavailable),
                    color = colors.danger, modifier = Modifier.testTag("progress_photo_load_error"))
            } else if (state.progressPhotos.isEmpty()) {
                Text(strings.getString(R.string.native_progress_photo_empty),
                    color = colors.muted, modifier = Modifier.testTag("progress_photo_empty"))
            } else {
                LazyRow(
                    state = rememberLazyListState(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("progress_photo_choices")
                ) {
                    items(state.progressPhotos, key = { it.id }) { photo ->
                        Column(
                            modifier = Modifier.clickable { open(photo.id) }
                                .testTag("progress_photo_${photo.id}").size(140.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PrivateProgressPhoto(photo.id, 256, state.language,
                                Modifier.size(112.dp).background(colors.card2, RoundedCornerShape(12.dp)))
                            val label = when (photo.label) {
                                "before" -> strings.getString(R.string.native_progress_photo_before)
                                "after" -> strings.getString(R.string.native_progress_photo_after)
                                else -> photo.label
                            }
                            Text(label.ifBlank { date.format(Instant.ofEpochMilli(photo.workoutStartedEpochMs)) },
                                color = colors.text, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    val selected = state.progressPhotos.firstOrNull { it.id == state.selectedPhotoId }
    if (selected != null) {
        Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(colors.bg).systemBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.getString(R.string.native_progress_photo_title),
                        color = colors.text, modifier = Modifier.weight(1f))
                    IconButton(onClick = close, modifier = Modifier.size(48.dp)
                        .testTag("progress_photo_close")) {
                        Icon(Icons.Default.Close,
                            contentDescription = strings.getString(R.string.native_progress_photo_close),
                            tint = colors.text)
                    }
                }
                Text(
                    "${strings.getString(R.string.native_progress_photo_workout)}: " +
                        listOf(selected.programName, selected.dayName, selected.originDevice)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                    color = colors.muted
                )
                Text(date.format(Instant.ofEpochMilli(selected.workoutStartedEpochMs)),
                    color = colors.muted)
                PrivateProgressPhoto(selected.id, 1600, state.language,
                    Modifier.fillMaxWidth().weight(1f).testTag("progress_photo_detail"))
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
