package com.kelsos.mbrc.feature.playback.player.compose

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.kelsos.mbrc.core.common.layout.WindowWidthClass
import com.kelsos.mbrc.core.common.state.LfmRating
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.state.Repeat
import com.kelsos.mbrc.core.common.state.ShuffleMode
import com.kelsos.mbrc.core.common.state.TrackInfo
import com.kelsos.mbrc.core.common.state.TrackRating
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.ui.R as CoreUiR
import com.kelsos.mbrc.core.ui.compose.DynamicScreenScaffold
import com.kelsos.mbrc.core.ui.compose.ThinSlider
import com.kelsos.mbrc.core.ui.compose.TopBarState
import com.kelsos.mbrc.core.ui.compose.WaveProgressIndicator
import com.kelsos.mbrc.core.ui.compose.trackTextMarquee
import com.kelsos.mbrc.feature.misc.output.compose.OutputSelectionBottomSheet
import com.kelsos.mbrc.feature.playback.R
import com.kelsos.mbrc.feature.playback.lyrics.LyricsViewModel
import com.kelsos.mbrc.feature.playback.lyrics.compose.LyricsScreenContent
import com.kelsos.mbrc.feature.playback.player.IPlayerActions
import com.kelsos.mbrc.feature.playback.player.PlaybackState
import com.kelsos.mbrc.feature.playback.player.PlayerViewModel
import com.kelsos.mbrc.feature.playback.player.VolumeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun PlayerScreen(
  onNavigateToNowPlaying: () -> Unit,
  onNavigateToAlbum: (album: String, artist: String) -> Unit,
  onNavigateToArtist: (artist: String) -> Unit,
  snackbarHostState: SnackbarHostState,
  onOpenDrawer: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PlayerViewModel = koinViewModel(),
  lyricsViewModel: LyricsViewModel = koinViewModel()
) {
  // Collect separate state flows for granular recomposition
  val playingTrack by viewModel.playingTrack.collectAsStateWithLifecycle()
  val playingPosition by viewModel.playingPosition.collectAsStateWithLifecycle()
  val trackRating by viewModel.trackRating.collectAsStateWithLifecycle()
  val volumeState by viewModel.volumeState.collectAsStateWithLifecycle()
  val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
  val isScrobbling by viewModel.isScrobbling.collectAsStateWithLifecycle()
  val trackDetails by viewModel.trackDetails.collectAsStateWithLifecycle()
  val showRatingOnPlayer by viewModel.showRatingOnPlayer.collectAsStateWithLifecycle()

  // Lyrics state
  val lyrics by lyricsViewModel.lyrics.collectAsStateWithLifecycle(initialValue = emptyList())
  val lyricsPlayingTrack by lyricsViewModel.playingTrack.collectAsStateWithLifecycle()
  val lyricsPlayingPosition by lyricsViewModel.playingPosition.collectAsStateWithLifecycle()
  val isPlaying by lyricsViewModel.isPlaying.collectAsStateWithLifecycle()

  var showBottomSheet by remember { mutableStateOf(false) }
  var showOutputSelection by remember { mutableStateOf(false) }
  var showLyrics by remember { mutableStateOf(false) }
  var showTrackDetails by remember { mutableStateOf(false) }

  val title = stringResource(R.string.nav_now_playing)

  // Handle back press to close lyrics overlay
  BackHandler(enabled = showLyrics) {
    showLyrics = false
  }

  // Compute scaffold configuration based on current state
  val topBarState = if (showLyrics) TopBarState.Hidden else TopBarState.WithTitle(title)
  // 3-dot menu opens bottom sheet directly (only when not showing lyrics)
  val onOverflowClick: (() -> Unit)? = if (showLyrics) {
    null
  } else {
    { showBottomSheet = true }
  }

  if (showBottomSheet) {
    PlayerBottomSheet(
      isScrobbling = isScrobbling,
      onScrobbleToggle = viewModel.actions.toggleScrobbling,
      onShowTrackDetails = { showTrackDetails = true },
      onGoToAlbum = playingTrack.album.takeIf { it.isNotEmpty() }?.let {
        { onNavigateToAlbum(playingTrack.album, playingTrack.artist) }
      },
      onGoToArtist = playingTrack.artist.takeIf { it.isNotEmpty() }?.let {
        { onNavigateToArtist(playingTrack.artist) }
      },
      onDismiss = { showBottomSheet = false },
      isBanned = trackRating.lfmRating == LfmRating.Banned,
      isStream = playingPosition.isStream,
      onBanClick = {
        viewModel.actions.toggleBan(
          trackRating.lfmRating == LfmRating.Banned,
          trackRating.lfmRating == LfmRating.Loved
        )
      }
    )
  }

  if (showOutputSelection) {
    OutputSelectionBottomSheet(
      onDismiss = { showOutputSelection = false }
    )
  }

  if (showTrackDetails) {
    TrackDetailsBottomSheet(
      trackDetails = trackDetails,
      onDismiss = { showTrackDetails = false }
    )
  }

  DynamicScreenScaffold(
    topBarState = topBarState,
    snackbarHostState = snackbarHostState,
    defaultTitle = title,
    onOpenDrawer = onOpenDrawer,
    onOverflowClick = onOverflowClick,
    isTransparent = true,
    modifier = modifier
  ) { paddingValues ->
    // The transparent top bar draws over the gradient, but the scaffold still
    // reports the status-bar + top-bar height as the top inset and the
    // navigation-bar / taskbar as the bottom inset. Thread that through so every
    // layout keeps its content clear of the system bars (#324/#325).
    Box(modifier = Modifier.fillMaxSize()) {
      PlayerScreenContent(
        playingTrack = playingTrack,
        playingPosition = playingPosition,
        trackRating = trackRating,
        volumeState = volumeState,
        playbackState = playbackState,
        contentPadding = paddingValues,
        actions = viewModel.actions,
        hasLyrics = lyrics.isNotEmpty(),
        showRatingOnPlayer = showRatingOnPlayer,
        onTrackInfoClick = onNavigateToNowPlaying,
        onLyricsClick = { showLyrics = true },
        onOutputClick = { showOutputSelection = true },
        onRatingClick = { showBottomSheet = true }
      )

      // Lyrics overlay with slide animation from bottom
      AnimatedVisibility(
        visible = showLyrics,
        enter = slideInVertically(
          initialOffsetY = { fullHeight -> fullHeight },
          animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
          targetOffsetY = { fullHeight -> fullHeight },
          animationSpec = tween(durationMillis = 300)
        )
      ) {
        LyricsScreenContent(
          lyrics = lyrics,
          playingTrack = lyricsPlayingTrack,
          playingPosition = lyricsPlayingPosition,
          composer = trackDetails.composer,
          isPlaying = isPlaying,
          onCollapse = { showLyrics = false },
          onPlayPauseClick = lyricsViewModel::playPause,
          onSeek = { lyricsViewModel.seek(it.toInt()) },
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

@Composable
fun PlayerScreenContent(
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
  onRatingClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp)
) {
  val darkTheme = isSystemInDarkTheme()
  val defaultBackground = MaterialTheme.colorScheme.background

  // Load album art and extract colors - single image load shared with AlbumCover
  val albumArtState = rememberAlbumArtState(
    coverUrl = playingTrack.coverUrl,
    defaultBackground = defaultBackground,
    darkTheme = darkTheme
  )

  // Animate color transitions smoothly when track changes
  val animatedDominant by animateColorAsState(
    targetValue = albumArtState.colors.dominant.copy(alpha = 0.6f),
    animationSpec = tween(durationMillis = 500),
    label = "dominant_color"
  )
  val animatedBackground by animateColorAsState(
    targetValue = albumArtState.colors.backgroundColor,
    animationSpec = tween(durationMillis = 500),
    label = "background_color"
  )

  // Remembered because the marquees keep the player drawing every frame, and an unremembered
  // brush reallocates on each of them for colours that only move while a track change animates.
  val gradientBrush = remember(animatedDominant, animatedBackground) {
    Brush.verticalGradient(colors = listOf(animatedDominant, animatedBackground))
  }

  val isFavorite = trackRating.lfmRating == LfmRating.Loved
  val isBanned = trackRating.lfmRating == LfmRating.Banned

  BoxWithConstraints(modifier = modifier) {
    // Measured from the space the player actually has rather than from the device orientation: a
    // tablet in portrait has more room than a phone in landscape, and branching on orientation
    // handed the phone-shaped layout to landscape tablets.
    val widthClass = WindowWidthClass.fromWidthDp(maxWidth.value.toInt())
    val isWide = maxWidth > maxHeight
    val contentMaxWidth = if (widthClass == WindowWidthClass.Compact) {
      Dp.Infinity
    } else {
      PlayerConstants.CONTROLS_MAX_WIDTH
    }

    when {
      isWide -> LandscapePlayerLayout(
        contentMaxWidth = contentMaxWidth,
        // A phone in landscape leaves roughly 300dp for the whole control stack, which the
        // default spacing overruns. A tablet in landscape has no such problem, so the tighter
        // spacing is keyed off height rather than applied to every wide layout.
        tightSpacing = maxHeight < PlayerConstants.SHORT_HEIGHT_THRESHOLD,
        painter = albumArtState.painter,
        playingTrack = playingTrack,
        playingPosition = playingPosition,
        isFavorite = isFavorite,
        isBanned = isBanned,
        hasLyrics = hasLyrics,
        rating = trackRating.rating,
        showRating = showRatingOnPlayer,
        volumeState = volumeState,
        playbackState = playbackState,
        gradientBrush = gradientBrush,
        contentPadding = contentPadding,
        actions = actions,
        onTrackInfoClick = onTrackInfoClick,
        onLyricsClick = onLyricsClick,
        onOutputClick = onOutputClick,
        onRatingClick = onRatingClick
      )

      else -> PortraitPlayerLayout(
        contentMaxWidth = contentMaxWidth,
        painter = albumArtState.painter,
        playingTrack = playingTrack,
        playingPosition = playingPosition,
        isFavorite = isFavorite,
        isBanned = isBanned,
        hasLyrics = hasLyrics,
        rating = trackRating.rating,
        showRating = showRatingOnPlayer,
        volumeState = volumeState,
        playbackState = playbackState,
        gradientBrush = gradientBrush,
        contentPadding = contentPadding,
        actions = actions,
        onTrackInfoClick = onTrackInfoClick,
        onLyricsClick = onLyricsClick,
        onOutputClick = onOutputClick,
        onRatingClick = onRatingClick
      )
    }
  }
}

/**
 * Constants for player layout dimensions and values.
 */
private object PlayerConstants {
  const val LANDSCAPE_ALBUM_HEIGHT_FRACTION = 0.85f
  val PORTRAIT_BOTTOM_PADDING = 16.dp
  const val VOLUME_MAX = 100f
  const val SLIDER_DEBOUNCE_MS = 1000L
  val CONTENT_PADDING = 24.dp

  /**
   * A blur this size is redrawn on every frame the marquees ask for, and the cover is the largest
   * thing on the screen. Material tops out around this for a raised surface anyway.
   */
  val COVER_ELEVATION = 8.dp

  /**
   * How wide the metadata and transport are allowed to get once the window is past compact. The
   * cover is deliberately not bound by this: it is the element that should grow with the screen.
   */
  val CONTROLS_MAX_WIDTH = 560.dp

  /** Below this the landscape control stack does not fit at its normal spacing. */
  val SHORT_HEIGHT_THRESHOLD = 420.dp
}

/**
 * Data class to hold colors extracted from album artwork.
 */
private data class AlbumColors(
  val dominant: Color,
  val vibrant: Color,
  val darkVibrant: Color,
  val backgroundColor: Color
)

/**
 * Data class to hold album art state including painter and extracted colors.
 */
private data class AlbumArtState(val painter: AsyncImagePainter, val colors: AlbumColors)

/**
 * Remembers album art state including the painter and extracted colors.
 * This ensures the image is only loaded once and shared between display and color extraction.
 */
@Composable
private fun rememberAlbumArtState(
  coverUrl: String,
  defaultBackground: Color,
  darkTheme: Boolean
): AlbumArtState {
  val context = LocalContext.current

  val painter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(context)
      .data(coverUrl.ifEmpty { null })
      .crossfade(true)
      .allowHardware(false) // Required for Palette color extraction
      .build()
  )

  // Collect painter state as a snapshot state for proper recomposition
  val painterState by painter.state.collectAsStateWithLifecycle()

  var colors by remember(defaultBackground) {
    mutableStateOf(
      AlbumColors(
        dominant = defaultBackground,
        vibrant = defaultBackground,
        darkVibrant = defaultBackground,
        backgroundColor = defaultBackground
      )
    )
  }

  // Skipped while inspecting: a preview has no Koin graph to inject the dispatchers from, and no
  // cover to extract anything out of either, so it keeps the default palette it starts with.
  if (LocalInspectionMode.current) {
    return AlbumArtState(painter = painter, colors = colors)
  }

  val dispatchers: AppCoroutineDispatchers = koinInject()

  // Update colors when theme changes or image loads
  LaunchedEffect(painterState, darkTheme, defaultBackground) {
    when (val currentState = painterState) {
      is AsyncImagePainter.State.Success -> {
        // Palette.generate() and the bitmap copy it needs both block, and LaunchedEffect runs on
        // the main dispatcher, so this stalled the UI thread on every track change.
        colors = withContext(dispatchers.io) {
          extractColorsFromBitmap(
            currentState.result.image.toBitmap(),
            defaultBackground,
            darkTheme
          )
        }
      }

      else -> {
        // Reset to default background when no image
        colors = AlbumColors(
          dominant = defaultBackground,
          vibrant = defaultBackground,
          darkVibrant = defaultBackground,
          backgroundColor = defaultBackground
        )
      }
    }
  }

  return AlbumArtState(painter = painter, colors = colors)
}

/**
 * Extracts colors from a bitmap using Android's Palette API.
 * Uses different palette swatches based on the current theme:
 * - Dark theme: Uses darker/muted colors
 * - Light theme: Uses lighter/vibrant colors
 */
private fun extractColorsFromBitmap(
  bitmap: Bitmap,
  defaultBackground: Color,
  darkTheme: Boolean
): AlbumColors {
  val palette = Palette.from(bitmap).generate()
  val defaultColor = defaultBackground.copy(alpha = 1f)
  val androidDefault = android.graphics.Color.argb(
    (defaultColor.alpha * 255).toInt(),
    (defaultColor.red * 255).toInt(),
    (defaultColor.green * 255).toInt(),
    (defaultColor.blue * 255).toInt()
  )

  // Choose appropriate palette swatch based on theme
  val dominant = if (darkTheme) {
    palette.getDarkMutedColor(palette.getDominantColor(androidDefault))
  } else {
    palette.getLightMutedColor(palette.getDominantColor(androidDefault))
  }

  val vibrant = if (darkTheme) {
    palette.getDarkVibrantColor(dominant)
  } else {
    palette.getLightVibrantColor(dominant)
  }

  val darkVibrant = palette.getDarkVibrantColor(dominant)

  return AlbumColors(
    dominant = Color(dominant),
    vibrant = Color(vibrant),
    darkVibrant = Color(darkVibrant),
    backgroundColor = defaultBackground
  )
}

@Composable
private fun PortraitPlayerLayout(
  contentMaxWidth: Dp,
  painter: AsyncImagePainter,
  playingTrack: TrackInfo,
  playingPosition: PlayingPosition,
  isFavorite: Boolean,
  isBanned: Boolean,
  hasLyrics: Boolean,
  rating: Float?,
  showRating: Boolean,
  volumeState: VolumeState,
  playbackState: PlaybackState,
  gradientBrush: Brush,
  contentPadding: PaddingValues,
  actions: IPlayerActions,
  onTrackInfoClick: () -> Unit,
  onLyricsClick: () -> Unit,
  onOutputClick: () -> Unit,
  onRatingClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .background(gradientBrush)
      // System insets from the scaffold: status-bar + top-bar on top, and the
      // navigation-bar / taskbar on the bottom keeps the volume row on screen (#324).
      .padding(contentPadding)
      .padding(
        top = PlayerConstants.CONTENT_PADDING,
        bottom = PlayerConstants.PORTRAIT_BOTTOM_PADDING
      ),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Album cover - takes the leftover vertical space and sizes itself to the
    // largest square that fits, so the metadata, transport and volume rows below
    // always stay on screen instead of being pushed off the bottom (#324/#325).
    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = PlayerConstants.CONTENT_PADDING),
      contentAlignment = Alignment.Center
    ) {
      AlbumCover(
        painter = painter,
        modifier = Modifier.size(minOf(maxWidth, maxHeight)),
        hasLyrics = hasLyrics,
        onLyricsClick = onLyricsClick
      )
    }

    Spacer(modifier = Modifier.height(32.dp))

    // The cover above is free to take the full width, but the metadata and transport below are
    // not: on a portrait tablet an 800dp seek bar and a title centred across the same span read
    // as a stretched phone rather than a layout.
    Column(
      modifier = Modifier
        .widthIn(max = contentMaxWidth)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Track info with the favourite button
      TrackInfoWithFavorite(
        track = playingTrack,
        isFavorite = isFavorite,
        isStream = playingPosition.isStream,
        onTrackClick = onTrackInfoClick,
        onFavoriteClick = { actions.toggleFavorite(isFavorite, isBanned) },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = PlayerConstants.CONTENT_PADDING)
      )

      // Rating display (optional)
      if (showRating) {
        Spacer(modifier = Modifier.height(12.dp))
        RatingDisplay(
          rating = rating,
          onClick = onRatingClick,
          modifier = Modifier.padding(horizontal = PlayerConstants.CONTENT_PADDING)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Progress bar
      ProgressSection(
        position = playingPosition,
        onSeek = actions.seek,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = PlayerConstants.CONTENT_PADDING)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Playback controls
      PlaybackControls(
        playbackState = playbackState,
        actions = actions
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Volume control - compact
      VolumeSection(
        volumeState = volumeState,
        actions = actions,
        onOutputClick = onOutputClick,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = PlayerConstants.CONTENT_PADDING)
      )
    }
  }
}

@Composable
private fun LandscapePlayerLayout(
  contentMaxWidth: Dp,
  tightSpacing: Boolean,
  painter: AsyncImagePainter,
  playingTrack: TrackInfo,
  playingPosition: PlayingPosition,
  isFavorite: Boolean,
  isBanned: Boolean,
  hasLyrics: Boolean,
  rating: Float?,
  showRating: Boolean,
  volumeState: VolumeState,
  playbackState: PlaybackState,
  gradientBrush: Brush,
  contentPadding: PaddingValues,
  actions: IPlayerActions,
  onTrackInfoClick: () -> Unit,
  onLyricsClick: () -> Unit,
  onOutputClick: () -> Unit,
  onRatingClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .background(gradientBrush)
      // System insets from the scaffold; in landscape this also clears a side
      // navigation bar, which the previous fixed padding did not (#324/#325).
      .padding(contentPadding)
      .padding(
        top = if (tightSpacing) 12.dp else PlayerConstants.CONTENT_PADDING,
        start = PlayerConstants.CONTENT_PADDING,
        end = PlayerConstants.CONTENT_PADDING,
        bottom = if (tightSpacing) 12.dp else PlayerConstants.CONTENT_PADDING
      ),
    horizontalArrangement = Arrangement.spacedBy(32.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left side - Album cover (constrained size)
    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center
    ) {
      AlbumCover(
        painter = painter,
        modifier = Modifier
          .fillMaxHeight(PlayerConstants.LANDSCAPE_ALBUM_HEIGHT_FRACTION)
          .aspectRatio(1f),
        hasLyrics = hasLyrics,
        onLyricsClick = onLyricsClick
      )
    }

    // Right side - Controls. The cap lives on an inner column rather than on the weighted one:
    // weight hands the child an exact width, which widthIn cannot then shrink.
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
      contentAlignment = Alignment.Center
    ) {
      // Scrollable because a phone in landscape leaves roughly 300dp of height for metadata,
      // rating, seek bar, transport and volume. Centred content that does not fit is clipped at
      // both ends, which is how the volume row disappeared in #324/#325.
      Column(
        modifier = Modifier
          .widthIn(max = contentMaxWidth)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Track info with the favourite button
        TrackInfoWithFavorite(
          track = playingTrack,
          isFavorite = isFavorite,
          isStream = playingPosition.isStream,
          onTrackClick = onTrackInfoClick,
          onFavoriteClick = { actions.toggleFavorite(isFavorite, isBanned) },
          modifier = Modifier.fillMaxWidth()
        )

        // Rating display (optional)
        if (showRating) {
          Spacer(modifier = Modifier.height(if (tightSpacing) 4.dp else 12.dp))
          RatingDisplay(
            rating = rating,
            onClick = onRatingClick
          )
        }

        Spacer(modifier = Modifier.height(if (tightSpacing) 12.dp else 24.dp))

        // Progress bar
        ProgressSection(
          position = playingPosition,
          onSeek = actions.seek,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(if (tightSpacing) 4.dp else 16.dp))

        // Playback controls
        PlaybackControls(
          playbackState = playbackState,
          actions = actions
        )

        Spacer(modifier = Modifier.height(if (tightSpacing) 8.dp else 24.dp))

        // Volume control
        VolumeSection(
          volumeState = volumeState,
          actions = actions,
          onOutputClick = onOutputClick,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

/**
 * The album cover, which doubles as the way into the lyrics overlay.
 *
 * Lyrics used to be an icon button on the track info row, where it took a 48dp touch target away
 * from a title that had 168dp to work with on a phone. The cover is the largest inert surface on
 * the screen and lyrics are something people want more prominent rather than less, so the gesture
 * lives here and the badge carries the availability signal the button's tint used to.
 */
@Composable
private fun AlbumCover(
  painter: AsyncImagePainter,
  modifier: Modifier = Modifier,
  hasLyrics: Boolean = false,
  onLyricsClick: (() -> Unit)? = null
) {
  val placeholderPainter = painterResource(CoreUiR.drawable.ic_image_no_cover)
  val painterState by painter.state.collectAsStateWithLifecycle()
  val lyricsLabel = stringResource(R.string.nav_lyrics)

  Surface(
    modifier = modifier
      .shadow(
        elevation = PlayerConstants.COVER_ELEVATION,
        shape = MaterialTheme.shapes.medium,
        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
      )
      .clip(MaterialTheme.shapes.medium)
      .then(
        if (onLyricsClick != null) {
          Modifier.clickable(onClickLabel = lyricsLabel, onClick = onLyricsClick)
        } else {
          Modifier
        }
      ),
    tonalElevation = 0.dp
  ) {
    val activePainter = when (painterState) {
      is AsyncImagePainter.State.Success -> painter
      is AsyncImagePainter.State.Error -> placeholderPainter
      else -> placeholderPainter
    }

    Box(modifier = Modifier.fillMaxSize()) {
      Image(
        painter = activePainter,
        contentDescription = stringResource(R.string.description_album_cover),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )

      // Only drawn when there is something to read, so its presence is the signal.
      if (hasLyrics && onLyricsClick != null) {
        Surface(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(12.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
          tonalElevation = 3.dp
        ) {
          Icon(
            imageVector = Icons.Outlined.Lyrics,
            contentDescription = lyricsLabel,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .padding(8.dp)
              .size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun TrackInfoWithFavorite(
  track: TrackInfo,
  isFavorite: Boolean,
  isStream: Boolean,
  onTrackClick: () -> Unit,
  onFavoriteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Track info - left aligned with fixed heights to prevent UI jumping
    Column(
      modifier = Modifier
        .weight(1f)
        .clickable(onClick = onTrackClick)
    ) {
      Text(
        text = track.title.ifEmpty { stringResource(R.string.unknown_title) },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .height(28.dp)
          .trackTextMarquee()
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = track.artist.ifEmpty { stringResource(R.string.unknown_artist) },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .height(22.dp)
          .trackTextMarquee()
      )

      Spacer(modifier = Modifier.height(2.dp))

      // Album with year - always shown with fixed height
      val albumText = if (track.album.isNotEmpty()) {
        if (track.year.isNotEmpty()) {
          "${track.album} • ${track.year}"
        } else {
          track.album
        }
      } else {
        " " // Space to maintain height
      }
      Text(
        text = albumText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .height(20.dp)
          .trackTextMarquee()
      )
    }

    // Favorite is the only action left on this row: it is the one people use often, and three
    // 48dp buttons took 144dp of a 312dp row away from the title. Ban moved to the overflow
    // sheet, lyrics to the album cover.
    // Favorite button - disabled for streams (LFM rating not applicable)
    IconButton(
      onClick = onFavoriteClick,
      enabled = !isStream
    ) {
      Icon(
        imageVector = if (isFavorite) {
          Icons.Default.Favorite
        } else {
          Icons.Default.FavoriteBorder
        },
        contentDescription = stringResource(R.string.player_lfm_love),
        tint = when {
          isStream -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
          isFavorite -> MaterialTheme.colorScheme.primary
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.size(28.dp)
      )
    }
  }
}

@Composable
private fun ProgressSection(
  position: PlayingPosition,
  onSeek: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var sliderPosition by remember { mutableFloatStateOf(0f) }
  var isUserSeeking by remember { mutableStateOf(false) }
  var ignoreServerUpdates by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  val isStream = position.isStream
  val totalMs = position.total.toFloat().coerceAtLeast(1f)
  val currentNormalized = if (isStream) 0f else position.current.toFloat() / totalMs

  LaunchedEffect(position.current) {
    if (!isUserSeeking && !ignoreServerUpdates && !isStream) {
      sliderPosition = currentNormalized
    }
  }

  Column(modifier = modifier) {
    if (isStream) {
      // For streams, show a wiggly wave indicator (not seekable)
      WaveProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface,
        backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
      )
    } else {
      ThinSlider(
        value = if (isUserSeeking || ignoreServerUpdates) sliderPosition else currentNormalized,
        onValueChange = {
          isUserSeeking = true
          sliderPosition = it
        },
        onValueChangeFinished = {
          onSeek((sliderPosition * totalMs).toInt())
          isUserSeeking = false
          ignoreServerUpdates = true
          // Keep ignoring server updates for a bit after release to prevent jumping
          scope.launch {
            delay(PlayerConstants.SLIDER_DEBOUNCE_MS)
            ignoreServerUpdates = false
          }
        },
        trackColor = MaterialTheme.colorScheme.onSurface,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        thumbColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = position.currentMinutes,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = position.totalMinutes,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun VolumeSection(
  volumeState: VolumeState,
  actions: IPlayerActions,
  onOutputClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var sliderPosition by remember { mutableFloatStateOf(0f) }
  var isUserDragging by remember { mutableStateOf(false) }
  var ignoreServerUpdates by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  val volumeNormalized = volumeState.volume.toFloat() / PlayerConstants.VOLUME_MAX

  LaunchedEffect(volumeState.volume) {
    if (!isUserDragging && !ignoreServerUpdates) {
      sliderPosition = volumeNormalized
    }
  }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      imageVector = if (volumeState.mute || volumeState.volume == 0) {
        Icons.AutoMirrored.Filled.VolumeOff
      } else {
        Icons.AutoMirrored.Filled.VolumeUp
      },
      contentDescription = stringResource(R.string.main_button_mute_description),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier
        .size(20.dp)
        .clickable(onClick = actions.mute)
    )

    // Thin volume slider with local state for smooth dragging
    ThinSlider(
      value = if (volumeState.mute) {
        0f
      } else if (isUserDragging || ignoreServerUpdates) {
        sliderPosition
      } else {
        volumeNormalized
      },
      onValueChange = {
        isUserDragging = true
        sliderPosition = it
        actions.changeVolume((it * PlayerConstants.VOLUME_MAX).toInt())
      },
      onValueChangeFinished = {
        isUserDragging = false
        ignoreServerUpdates = true
        // Keep ignoring server updates for a bit after release to prevent jumping
        scope.launch {
          delay(PlayerConstants.SLIDER_DEBOUNCE_MS)
          ignoreServerUpdates = false
        }
      },
      trackColor = MaterialTheme.colorScheme.onSurface,
      inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
      thumbColor = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )

    // Output selection button
    IconButton(
      onClick = onOutputClick,
      modifier = Modifier.size(32.dp)
    ) {
      Icon(
        imageVector = Icons.Default.SpeakerGroup,
        contentDescription = stringResource(R.string.output_selection_title),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

@Composable
private fun PlaybackControls(playbackState: PlaybackState, actions: IPlayerActions) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Shuffle button
    ShuffleButton(
      shuffleMode = playbackState.shuffle,
      onClick = actions.shuffle
    )

    // Previous button - larger
    IconButton(
      onClick = actions.previous,
      modifier = Modifier.size(56.dp)
    ) {
      Icon(
        imageVector = Icons.Default.SkipPrevious,
        contentDescription = stringResource(R.string.main_button_previous_description),
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(36.dp)
      )
    }

    // Play/Pause button - large filled circle
    // Tap toggles play/pause, long-press stops playback (restores the legacy gesture).
    // FilledIconButton has no onLongClick, so we render the same filled circle with a Surface
    // (identical shape rasterization) and drive both gestures via combinedClickable.
    val haptics = LocalHapticFeedback.current
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.onSurface,
      contentColor = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .size(64.dp)
        .combinedClickable(
          onClick = actions.playPause,
          onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.stop()
          },
          onLongClickLabel = stringResource(R.string.main_button_stop_description)
        )
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = if (playbackState.playerState == PlayerState.Playing) {
            Icons.Default.Pause
          } else {
            Icons.Default.PlayArrow
          },
          contentDescription = stringResource(R.string.main_button_play_pause_description),
          modifier = Modifier.size(32.dp)
        )
      }
    }

    // Next button - larger
    IconButton(
      onClick = actions.next,
      modifier = Modifier.size(56.dp)
    ) {
      Icon(
        imageVector = Icons.Default.SkipNext,
        contentDescription = stringResource(R.string.main_button_next_description),
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(36.dp)
      )
    }

    // Repeat button
    IconButton(onClick = actions.repeat) {
      Icon(
        imageVector = if (playbackState.repeat == Repeat.One) {
          Icons.Default.RepeatOne
        } else {
          Icons.Default.Repeat
        },
        contentDescription = stringResource(R.string.main_button_repeat_description),
        tint = if (playbackState.repeat != Repeat.None) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
private fun ShuffleButton(shuffleMode: ShuffleMode, onClick: () -> Unit) {
  val isActive = shuffleMode != ShuffleMode.Off
  val isAutoDj = shuffleMode == ShuffleMode.AutoDJ

  IconButton(onClick = onClick) {
    if (isAutoDj) {
      Icon(
        imageVector = Icons.Default.Headset,
        contentDescription = stringResource(R.string.main_button_shuffle_description),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
      )
    } else {
      Icon(
        imageVector = Icons.Default.Shuffle,
        contentDescription = stringResource(R.string.main_button_shuffle_description),
        tint = if (isActive) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.size(24.dp)
      )
    }
  }
}
