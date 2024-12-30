
package com.kelsos.mbrc.services

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.kelsos.mbrc.annotations.Connection
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.ConnectionStatusChangeEvent
import com.kelsos.mbrc.events.ui.PlayStateChange
import com.kelsos.mbrc.events.ui.RemoteClientMetaData
import com.kelsos.mbrc.utilities.MediaButtonReceiver
import com.kelsos.mbrc.utilities.MediaIntentHandler
import com.kelsos.mbrc.utilities.RemoteUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSessionManager
  @Inject
  constructor(
    context: Application,
    volumeProvider: RemoteVolumeProvider,
    private val bus: RxBus,
  ) : AudioManager.OnAudioFocusChangeListener {
    private val mediaSession: MediaSessionCompat?

    @Inject
    lateinit var handler: MediaIntentHandler

    init {
      bus.register(this, RemoteClientMetaData::class.java) { this.metadataUpdate(it) }
      bus.register(this, PlayStateChange::class.java) { this.updateState(it) }
      bus.register(
        this,
        ConnectionStatusChangeEvent::class.java,
      ) { this.onConnectionStatusChanged(it) }

      val myEventReceiver = ComponentName(context.packageName, MediaButtonReceiver::class.java.name)
      val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
      mediaButtonIntent.component = myEventReceiver
      val mediaPendingIntent =
        PendingIntent.getBroadcast(
          context.applicationContext,
          0,
          mediaButtonIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

      mediaSession = MediaSessionCompat(context, "Session", myEventReceiver, mediaPendingIntent)
      mediaSession.setPlaybackToRemote(volumeProvider)
      mediaSession.setCallback(
        object : MediaSessionCompat.Callback() {
          override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            val success = handler.handleMediaIntent(mediaButtonEvent)
            return success || super.onMediaButtonEvent(mediaButtonEvent)
          }

          override fun onPlay() {
            postAction(UserAction(Protocol.PlayerPlay, true))
          }

          override fun onPause() {
            postAction(UserAction(Protocol.PlayerPause, true))
          }

          override fun onSkipToNext() {
            postAction(UserAction(Protocol.PlayerNext, true))
          }

          override fun onSkipToPrevious() {
            postAction(UserAction(Protocol.PlayerPrevious, true))
          }

          override fun onStop() {
            postAction(UserAction(Protocol.PlayerStop, true))
          }

          override fun onSeekTo(pos: Long) {
            postAction(UserAction.create(Protocol.NowPlayingPosition, pos))
          }
        },
      )
    }

    private fun onConnectionStatusChanged(event: ConnectionStatusChangeEvent) {
      if (event.status == Connection.OFF) {
        if (mediaSession != null) {
          val builder = PlaybackStateCompat.Builder()
          builder.setState(PlaybackStateCompat.STATE_STOPPED, -1, 0f)
          val playbackState = builder.build()
          mediaSession.isActive = false
          mediaSession.setPlaybackState(playbackState)
        }
      }
    }

    private fun postAction(action: UserAction) {
      bus.post(MessageEvent(ProtocolEventType.UserAction, action))
    }

    val mediaSessionToken: MediaSessionCompat.Token
      get() = mediaSession!!.sessionToken

    private fun metadataUpdate(data: RemoteClientMetaData) {
      if (mediaSession == null) {
        return
      }

      val trackInfo = data.trackInfo
      val bitmap = RemoteUtils.coverBitmapSync(data.coverPath)

      val builder =
        MediaMetadataCompat
          .Builder()
          .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, trackInfo.album)
          .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, trackInfo.artist)
          .putString(MediaMetadataCompat.METADATA_KEY_TITLE, trackInfo.title)
          .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
          .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, data.duration)
      mediaSession.setMetadata(builder.build())
    }

    private fun updateState(change: PlayStateChange) {
      if (mediaSession == null) {
        return
      }

      val builder = PlaybackStateCompat.Builder()
      builder.setActions(PLAYBACK_ACTIONS)
      when (change.state) {
        PlayerState.PLAYING -> {
          builder.setState(PlaybackStateCompat.STATE_PLAYING, change.position, 1f)
          mediaSession.isActive = true
        }
        PlayerState.PAUSED -> {
          builder.setState(PlaybackStateCompat.STATE_PAUSED, change.position, 0f)
          mediaSession.isActive = true
        }
        else -> {
          builder.setState(PlaybackStateCompat.STATE_STOPPED, change.position, 0f)
          mediaSession.isActive = false
        }
      }
      mediaSession.setPlaybackState(builder.build())
    }

    override fun onAudioFocusChange(focusChange: Int) {
      when (focusChange) {
        AudioManager.AUDIOFOCUS_GAIN -> Timber.d("gained")
        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> Timber.d("transient loss")
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Timber.d("Loss can duck")
      }
    }

    companion object {
      private const val PLAYBACK_ACTIONS =
        PlaybackStateCompat.ACTION_PAUSE or
          PlaybackStateCompat.ACTION_PLAY_PAUSE or
          PlaybackStateCompat.ACTION_PLAY or
          PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
          PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
          PlaybackStateCompat.ACTION_STOP or
          PlaybackStateCompat.ACTION_SEEK_TO
    }
  }
