package com.lyq2010.leesmusic.playback

import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var preferences: PlaybackPreferences
    private lateinit var policy: PlaybackNetworkPolicy
    private lateinit var sleepTimer: SleepTimer
    private lateinit var crossfade: CrossfadePlayer
    private lateinit var managedPlayer: ManagedPlayer
    private lateinit var notificationLyrics: NotificationLyrics
    private lateinit var pipeline: PlaybackPipeline
    private lateinit var recoveringPlayer: RecoveringPlayer
    private val handler = Handler(Looper.getMainLooper())
    private val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> handler.post { applyPreferences() } }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { handler.post { recoveringPlayer.networkRouteChanged() } }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { handler.post { recoveringPlayer.networkChanged() } }
        override fun onLost(network: Network) { handler.post { recoveringPlayer.networkChanged() } }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = PlaybackPreferences(this)
        policy = PlaybackNetworkPolicy(this)
        pipeline = PlaybackPipeline(this, policy)
        fun createPlayer() = ExoPlayer.Builder(this).setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(DefaultMediaSourceFactory(pipeline.factory)
                .setLoadErrorHandlingPolicy(StreamRetry.loadPolicy))
            .build()
        sleepTimer = SleepTimer(packageName)
        crossfade = CrossfadePlayer(createPlayer(), createPlayer(), preferences)
        recoveringPlayer = RecoveringPlayer(crossfade, policy::online,
            { !policy.blocked() || pipeline.fullyCached(crossfade.currentMediaItem) }, pipeline::refreshConnections,
            pipeline::prefetch, pipeline::cancelPrefetch)
        managedPlayer = ManagedPlayer(recoveringPlayer, preferences)
        mediaSession = MediaSession.Builder(this, managedPlayer).setCallback(sleepTimer).build()
        notificationLyrics = NotificationLyrics(this, managedPlayer, preferences)
        preferences.prefs.registerOnSharedPreferenceChangeListener(changed)
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)
        applyPreferences()
    }

    private fun applyPreferences() {
        crossfade.applyPreferences()
        recoveringPlayer.preferencesChanged()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        preferences.prefs.unregisterOnSharedPreferenceChangeListener(changed)
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
        handler.removeCallbacksAndMessages(null)
        sleepTimer.close()
        notificationLyrics.close()
        mediaSession?.run { player.release(); release() }
        pipeline.close()
        mediaSession = null
        super.onDestroy()
    }
}
