package com.kelsos.mbrc.screenshots

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.LfmRating
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.state.Repeat
import com.kelsos.mbrc.core.common.state.ShuffleMode
import com.kelsos.mbrc.core.common.state.TrackInfo
import com.kelsos.mbrc.core.common.state.TrackRating
import com.kelsos.mbrc.core.ui.compose.DynamicScreenScaffold
import com.kelsos.mbrc.core.ui.compose.TopBarState
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

/**
 * Renders the player exactly as it ships: inside the transparent [DynamicScreenScaffold]
 * so the recorded screenshot includes the "Now playing" top bar and the content sits
 * below it via the scaffold's [contentPadding]. Rendering [PlayerScreenContent] alone
 * cropped the header, which made the top spacing impossible to judge.
 */
@Composable
private fun PlayerScreenPreview(
  playingTrack: TrackInfo,
  playingPosition: PlayingPosition,
  trackRating: TrackRating,
  volumeState: VolumeState,
  playbackState: PlaybackState,
  actions: IPlayerActions,
  hasLyrics: Boolean,
  showRatingOnPlayer: Boolean,
  onTrackInfoClick: () -> Unit,
  onLyricsClick: () -> Unit,
  onOutputClick: () -> Unit,
  onRatingClick: () -> Unit
) {
  val snackbarHostState = remember { SnackbarHostState() }
  DynamicScreenScaffold(
    topBarState = TopBarState.WithTitle("Now playing"),
    snackbarHostState = snackbarHostState,
    defaultTitle = "Now playing",
    isTransparent = true,
    onOverflowClick = {}
  ) { paddingValues ->
    PlayerScreenContent(
      contentPadding = paddingValues,
      playingTrack = playingTrack,
      playingPosition = playingPosition,
      trackRating = trackRating,
      volumeState = volumeState,
      playbackState = playbackState,
      actions = actions,
      hasLyrics = hasLyrics,
      showRatingOnPlayer = showRatingOnPlayer,
      onTrackInfoClick = onTrackInfoClick,
      onLyricsClick = onLyricsClick,
      onOutputClick = onOutputClick,
      onRatingClick = onRatingClick
    )
  }
}

// =============================================================================
// Phone Previews - Portrait
// =============================================================================

@PreviewTest
@Preview(name = "Player Playing Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPlayingLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Playing Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPlayingDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Paused Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPausedLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePausedTrack(),
      playingPosition = PlayingPosition(current = 180000, total = 354000),
      trackRating = sampleLovedRating(),
      volumeState = VolumeState(volume = 50, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Paused),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Paused Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerPausedDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePausedTrack(),
      playingPosition = PlayingPosition(current = 180000, total = 354000),
      trackRating = sampleLovedRating(),
      volumeState = VolumeState(volume = 50, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Paused),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

// =============================================================================
// Common Device Sizes (representative sample of the main Playing state)
//
// Compact phone (360x720) is covered above and tablet/landscape below; these add
// a small phone, a large modern phone, and a foldable inner screen in portrait.
// The foldable is < 600dp wide on purpose so it exercises the portrait layout
// branch (not the tablet one) - the exact case behind #324/#325.
// =============================================================================

@PreviewTest
@Preview(
  name = "Player Small Phone",
  showBackground = true,
  device = "spec:width=360dp,height=640dp"
)
@Composable
fun PlayerSmallPhonePreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(
  name = "Player Large Phone",
  showBackground = true,
  device = "spec:width=412dp,height=915dp"
)
@Composable
fun PlayerLargePhonePreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(
  name = "Player Foldable Portrait",
  showBackground = true,
  device = "spec:width=585dp,height=760dp"
)
@Composable
fun PlayerFoldablePortraitPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
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
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Tablet Dark", showBackground = true, device = Devices.TABLET)
@Composable
fun PlayerTabletDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

// A tablet held in portrait: past the compact breakpoint, but taller than it is wide, so it takes
// the portrait layout with a capped control column rather than the side-by-side one.
@PreviewTest
@Preview(
  name = "Player Tablet Portrait Light",
  showBackground = true,
  widthDp = 800,
  heightDp = 1280
)
@Composable
fun PlayerTabletPortraitLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(
  name = "Player Tablet Portrait Dark",
  showBackground = true,
  widthDp = 800,
  heightDp = 1280
)
@Composable
fun PlayerTabletPortraitDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
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
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Landscape Dark", showBackground = true, widthDp = 720, heightDp = 360)
@Composable
fun PlayerLandscapeDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
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
    PlayerScreenPreview(
      playingTrack = sampleMutedTrack(),
      playingPosition = PlayingPosition(current = 240000, total = 482000),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 60, mute = true),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Muted Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerMutedDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = sampleMutedTrack(),
      playingPosition = PlayingPosition(current = 240000, total = 482000),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 60, mute = true),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Shuffle Repeat Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerShuffleRepeatLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
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
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Shuffle Repeat Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerShuffleRepeatDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
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
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Empty Track Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerEmptyTrackLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = BasicTrackInfo(),
      playingPosition = PlayingPosition(),
      trackRating = TrackRating(),
      volumeState = VolumeState(),
      playbackState = PlaybackState(),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Empty Track Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerEmptyTrackDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = BasicTrackInfo(),
      playingPosition = PlayingPosition(),
      trackRating = TrackRating(),
      volumeState = VolumeState(),
      playbackState = PlaybackState(),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

// =============================================================================
// Rating States Previews
// =============================================================================

@PreviewTest
@Preview(name = "Player Bomb Rating Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerBombRatingLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleBombRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Bomb Rating Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerBombRatingDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleBombRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(
  name = "Player Half Star Rating Light",
  showBackground = true,
  widthDp = 360,
  heightDp = 720
)
@Composable
fun PlayerHalfStarRatingLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleHalfStarRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(
  name = "Player Half Star Rating Dark",
  showBackground = true,
  widthDp = 360,
  heightDp = 720
)
@Composable
fun PlayerHalfStarRatingDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleHalfStarRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Unrated Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerUnratedLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleUnratedRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Unrated Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerUnratedDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleUnratedRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
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
    PlayerScreenPreview(
      playingTrack = sampleStreamTrack(),
      playingPosition = PlayingPosition(current = 125000, total = -1), // Stream (shows wave)
      trackRating = TrackRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Stream Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PlayerStreamDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = sampleStreamTrack(),
      playingPosition = PlayingPosition(current = 125000, total = -1), // Stream (shows wave)
      trackRating = TrackRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

// =============================================================================
// Short Viewport Previews (regression guard for #324/#325)
//
// On short usable heights the album cover must shrink so the volume row stays
// visible instead of being pushed off the bottom of the screen. These heights
// approximate a Galaxy Note 8 usable area and an even shorter viewport.
// =============================================================================

@PreviewTest
@Preview(name = "Player Short Height Light", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
fun PlayerShortHeightLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(name = "Player Short Height Dark", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
fun PlayerShortHeightDarkPreview() {
  RemoteTheme(darkTheme = true) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(
  name = "Player Very Short Height Light",
  showBackground = true,
  widthDp = 360,
  heightDp = 600
)
@Composable
fun PlayerVeryShortHeightLightPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
    )
  }
}

@PreviewTest
@Preview(
  name = "Player Short Height Large Font",
  showBackground = true,
  widthDp = 360,
  heightDp = 640,
  fontScale = 1.8f
)
@Composable
fun PlayerShortHeightLargeFontPreview() {
  RemoteTheme(darkTheme = false) {
    PlayerScreenPreview(
      playingTrack = samplePlayingTrack(),
      playingPosition = samplePlayingPosition(),
      trackRating = sampleNormalRating(),
      volumeState = VolumeState(volume = 75, mute = false),
      playbackState = PlaybackState(playerState = PlayerState.Playing),
      actions = PreviewPlayerActions,
      hasLyrics = false,
      showRatingOnPlayer = true,
      onTrackInfoClick = {},
      onLyricsClick = {},
      onOutputClick = {},
      onRatingClick = {}
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

private fun sampleBombRating() = TrackRating(
  lfmRating = LfmRating.Normal,
  rating = 0f
)

private fun sampleHalfStarRating() = TrackRating(
  lfmRating = LfmRating.Normal,
  rating = 3.5f
)

private fun sampleUnratedRating() = TrackRating(
  lfmRating = LfmRating.Normal,
  rating = null
)
