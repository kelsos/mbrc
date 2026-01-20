package com.kelsos.mbrc.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.LfmRating
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.state.Repeat
import com.kelsos.mbrc.core.common.state.ShuffleMode
import com.kelsos.mbrc.core.common.state.TrackRating
import com.kelsos.mbrc.core.ui.theme.RemoteTheme
import com.kelsos.mbrc.feature.playback.player.IPlayerActions
import com.kelsos.mbrc.feature.playback.player.PlaybackState
import com.kelsos.mbrc.feature.playback.player.VolumeState
import com.kelsos.mbrc.feature.playback.player.compose.PlayerScreenContent

/**
 * No-op implementation of IPlayerActions for preview purposes.
 */
private object PreviewPlayerActions : IPlayerActions {
  override val playPause: () -> Unit = {}
  override val previous: () -> Unit = {}
  override val next: () -> Unit = {}
  override val stop: () -> Unit = {}
  override val shuffle: () -> Unit = {}
  override val repeat: () -> Unit = {}
  override val mute: () -> Unit = {}
  override val changeVolume: (Int) -> Unit = {}
  override val seek: (Int) -> Unit = {}
  override val toggleFavorite: (Boolean, Boolean) -> Unit = { _, _ -> }
  override val toggleBan: (Boolean, Boolean) -> Unit = { _, _ -> }
  override val toggleScrobbling: () -> Unit = {}
}

// =============================================================================
// Phone Previews - Portrait
// =============================================================================

@PreviewTest
@Preview(name = "Player Playing Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPlayingLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Playing Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPlayingDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Paused Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPausedLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = samplePausedTrack(),
      playingPosition = PlayingPosition(current = 180000, total = 354000),
      trackRating = sampleLovedRating(),
      volumeState = VolumeState(volume = 50, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Paused),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Paused Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPausedDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = samplePausedTrack(),
      playingPosition = PlayingPosition(current = 180000, total = 354000),
      trackRating = sampleLovedRating(),
      volumeState = VolumeState(volume = 50, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Paused),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

// =============================================================================
// Tablet Previews
// =============================================================================

@PreviewTest
@Preview(name = "Player Tablet Light", showBackground = true, device = Devices.TABLET)
@Composable
fun PlayerTabletLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Tablet Dark", showBackground = true, device = Devices.TABLET)
@Composable
fun PlayerTabletDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

// =============================================================================
// Landscape Previews
// =============================================================================

@PreviewTest
@Preview(name = "Player Landscape Light", showBackground = true, widthDp = 720, heightDp = 360)
@Composable
fun PlayerLandscapeLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Landscape Dark", showBackground = true, widthDp = 720, heightDp = 360)
@Composable
fun PlayerLandscapeDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

// =============================================================================
// Special States Previews
// =============================================================================

@PreviewTest
@Preview(name = "Player Muted Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerMutedLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = sampleMutedTrack(),
      playingPosition = PlayingPosition(current = 240000, total = 482000),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 60, mute = true),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Muted Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerMutedDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = sampleMutedTrack(),
      playingPosition = PlayingPosition(current = 240000, total = 482000),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 60, mute = true),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Shuffle Repeat Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerShuffleRepeatLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = sampleShuffleTrack(),
      playingPosition = PlayingPosition(current = 60000, total = 431000),
      trackRating = sampleLovedRating(),
      volumeState = VolumeState(volume = 80, mute = false),
      playbackState = PlaybackState(
        playerState = PlayerState.Playing,
        shuffle = ShuffleMode.Shuffle,
        repeat = Repeat.One
      ),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Shuffle Repeat Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerShuffleRepeatDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = sampleShuffleTrack(),
      playingPosition = PlayingPosition(current = 60000, total = 431000),
      trackRating = sampleLovedRating(),
      volumeState = VolumeState(volume = 80, mute = false),
      playbackState = PlaybackState(
        playerState = PlayerState.Playing,
        shuffle = ShuffleMode.Shuffle,
        repeat = Repeat.One
      ),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Empty Track Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerEmptyTrackLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = BasicTrackInfo(),
      playingPosition = PlayingPosition(),
      trackRating = TrackRating(),
      volumeState = VolumeState(),
      playbackState = PlaybackState(),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Empty Track Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerEmptyTrackDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = BasicTrackInfo(),
      playingPosition = PlayingPosition(),
      trackRating = TrackRating(),
      volumeState = VolumeState(),
      playbackState = PlaybackState(),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

// =============================================================================
// Stream Previews (Wave Indicator)
// =============================================================================

@PreviewTest
@Preview(name = "Player Stream Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerStreamLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenContent(
      playingTrack = sampleStreamTrack(),
      playingPosition = PlayingPosition(current = 125000, total = -1), // Stream (shows wave)
      trackRating = TrackRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Stream Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerStreamDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenContent(
      playingTrack = sampleStreamTrack(),
      playingPosition = PlayingPosition(current = 125000, total = -1), // Stream (shows wave)
      trackRating = TrackRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {}
    )
  }
}

// =============================================================================
// Sample Data Helpers
// =============================================================================

private fun samplePlayingTrack() = BasicTrackInfo(
  artist = "Pink Floyd",
  title = "Time",
  album = "The Dark Side of the Moon",
  year = "1973",
  path = "/music/time.mp3",
  coverUrl = ""
)

private fun samplePlayingPosition() = PlayingPosition(
  current = 125000,
  total = 413000
)

private fun samplePausedTrack() = BasicTrackInfo(
  artist = "Queen",
  title = "Bohemian Rhapsody",
  album = "A Night at the Opera",
  year = "1975",
  path = "/music/bohemian.mp3",
  coverUrl = ""
)

private fun sampleMutedTrack() = BasicTrackInfo(
  artist = "Led Zeppelin",
  title = "Stairway to Heaven",
  album = "Led Zeppelin IV",
  year = "1971",
  path = "/music/stairway.mp3",
  coverUrl = ""
)

private fun sampleShuffleTrack() = BasicTrackInfo(
  artist = "The Beatles",
  title = "Hey Jude",
  album = "Hey Jude",
  year = "1968",
  path = "/music/heyjude.mp3",
  coverUrl = ""
)

private fun sampleStreamTrack() = BasicTrackInfo(
  artist = "1.FM",
  title = "Absolute 90s Party Zone",
  album = "Internet Radio",
  year = "",
  path = "http://stream.1fm.com/90s",
  coverUrl = ""
)

private fun sampleNormalRating() = TrackRating(
  lfmRating = LfmRating.Normal,
  rating = 4f
)

private fun sampleLovedRating() = TrackRating(
  lfmRating = LfmRating.Loved,
  rating = 5f
)
