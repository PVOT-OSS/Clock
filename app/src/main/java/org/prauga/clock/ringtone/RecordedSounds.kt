/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ringtone

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateFormat
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import org.prauga.clock.R

/**
 * Sounds recorded by a recorder app (MediaStore.Audio.Media.RECORD_SOUND_ACTION) for use as alarm
 * sounds. The recorder only lends its result, so each recording is copied into the app's files and
 * served from our own FileProvider; those uris need no persisted grant.
 */
object RecordedSounds {

    private const val DIR = "sounds"

    @JvmStatic fun recordIntent(): Intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)

    @JvmStatic
    fun canRecord(context: Context): Boolean =
        recordIntent().resolveActivity(context.packageManager) != null

    private fun authority(context: Context) = "${context.packageName}.sounds"

    /** Whether [uri] is one of our copied recordings. */
    @JvmStatic
    fun isRecordedSound(context: Context, uri: Uri): Boolean = uri.authority == authority(context)

    /** Copies the recording at [source] and returns its uri and title; null if it can't be read. */
    @JvmStatic
    fun importRecording(context: Context, source: Uri): Pair<Uri, String>? {
        val resolver = context.contentResolver
        val extension =
            MimeTypeMap.getSingleton().getExtensionFromMimeType(resolver.getType(source)) ?: "m4a"
        val now = System.currentTimeMillis()
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val file = File(dir, "recording_$now.$extension")
        try {
            resolver.openInputStream(source)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
        } catch (_: Exception) {
            file.delete()
            return null
        }
        val title =
            context.getString(
                R.string.recorded_sound_title,
                DateFormat.getMediumDateFormat(context).format(now) +
                    " " +
                    DateFormat.getTimeFormat(context).format(now),
            )
        return FileProvider.getUriForFile(context, authority(context), file) to title
    }

    /** Deletes the file behind a recorded sound uri; other uris are left alone. */
    @JvmStatic
    fun delete(context: Context, uri: Uri) {
        if (isRecordedSound(context, uri)) {
            uri.lastPathSegment?.let { File(File(context.filesDir, DIR), it).delete() }
        }
    }
}
