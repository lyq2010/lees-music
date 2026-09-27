package com.lyq2010.leesmusic.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.ui.catalog.LibrarySong

internal fun playbackMediaItem(server: SubsonicServer, song: LibrarySong, bitRate: Int,
                               client: SubsonicClient = SubsonicClient()): MediaItem {
        return MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(song.localUri ?: client.streamUrl(server, song.id, bitRate))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album)
                    .setArtworkUri(song.coverUrl?.let(android.net.Uri::parse))
                    .setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(Bundle().apply {
                        putString("namespace", com.lyq2010.leesmusic.data.library.serverCacheIdentity(server.baseUrl, server.username, "Navidrome"))
                        putString("coverArtId", song.coverArtId)
                        putString("coverUrl", song.coverUrl)
                        putInt("duration", song.duration)
                        putInt("track", song.track)
                        putString("suffix", song.suffix)
                        putInt("bitRate", song.bitRate)
                        putString("albumId", song.albumId)
                        putString("artistId", song.artistId)
                        putString("localUri", song.localUri)
                    }).build())
                .build()
    }


internal fun mediaItemSong(item: MediaItem): LibrarySong {
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        return LibrarySong(
            id = item.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            coverArtId = extras?.getString("coverArtId"),
            coverUrl = extras?.getString("coverUrl"),
            duration = extras?.getInt("duration") ?: 0,
            track = extras?.getInt("track") ?: 0,
            suffix = extras?.getString("suffix").orEmpty(),
            bitRate = extras?.getInt("bitRate") ?: 0,
            albumId = extras?.getString("albumId"),
            album = metadata.albumTitle?.toString().orEmpty(),
            artistId = extras?.getString("artistId"),
            localUri = extras?.getString("localUri"),
        )
    }
