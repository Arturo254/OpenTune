/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.utils

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.SongArtistMap
import com.arturo254.opentune.db.entities.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Stable id prefix for songs sourced from on-device MediaStore audio. */
private const val LOCAL_SONG_ID_PREFIX = "LM"

/**
 * Scans the device's [MediaStore.Audio.Media] for playable audio files and upserts them into
 * the app's [MusicDatabase] as regular [SongEntity] rows (isLocal = true), so they show up in
 * the library/liked-songs UI alongside YouTube Music tracks.
 */
object LocalMediaScanner {

    suspend fun scan(context: Context, database: MusicDatabase): Int = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        var count = 0
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mediaStoreId,
                )
                val title = cursor.getString(titleCol) ?: continue
                val artistName = cursor.getString(artistCol)?.takeUnless {
                    it == "<unknown>" || it.isBlank()
                } ?: "Unknown artist"
                val albumName = cursor.getString(albumCol)
                val durationMs = cursor.getLong(durationCol)
                val dateModifiedSec = cursor.getLong(dateModifiedCol)

                val songId = "$LOCAL_SONG_ID_PREFIX$mediaStoreId"
                val now = LocalDateTime.now()
                val dateModified = if (dateModifiedSec > 0) {
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(dateModifiedSec), ZoneId.systemDefault())
                } else {
                    null
                }

                database.withTransaction {
                    val existing = song(songId).first()

                    upsert(
                        SongEntity(
                            id = songId,
                            title = title,
                            duration = if (durationMs > 0) (durationMs / 1000).toInt() else -1,
                            albumName = albumName,
                            dateModified = dateModified,
                            inLibrary = existing?.song?.inLibrary ?: now,
                            liked = existing?.song?.liked ?: false,
                            likedDate = existing?.song?.likedDate,
                            isLocal = true,
                            localPath = contentUri.toString(),
                        ),
                    )

                    if (existing == null) {
                        val artistId = artistByName(artistName)?.id
                            ?: ArtistEntity.generateArtistId()
                        insert(ArtistEntity(id = artistId, name = artistName))
                        insert(SongArtistMap(songId = songId, artistId = artistId, position = 0))
                    }
                }

                count++
            }
        }
        count
    }
}
