package com.ermakov.nd.muapp.data.datasource

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.ermakov.nd.muapp.model.Audio
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LocalAudioDataSource @Inject constructor(
    @ApplicationContext private val context: Context
): AudioDataSource {
    override suspend fun getAudio(): List<Audio> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
        val audioList = mutableListOf<Audio>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(pathColumn)
                audioList += Audio(
                    id = id,
                    name = name,
                    path = path,
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                )
            }
        }
        return audioList
    }
}