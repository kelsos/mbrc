package com.kelsos.mbrc.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Main navigation graph for the MusicBee Remote app.
 * Defines all screen destinations and their navigation arguments.
 */
@Composable
fun AppNavGraph(navController: NavHostController, startDestination: String = Screen.Home.route) {
  NavHost(
    navController = navController,
    startDestination = startDestination
  ) {
    // Main screens accessible from drawer
    composable(Screen.Home.route) {
      // TODO: Implement HomeScreen (Player/Now Playing)
      PlaceholderScreen("Now Playing")
    }

    composable(Screen.Library.route) {
      // TODO: Implement LibraryScreen with tabs
      PlaceholderScreen("Library")
    }

    composable(Screen.Playlists.route) {
      // TODO: Implement PlaylistsScreen
      PlaceholderScreen("Playlists")
    }

    composable(Screen.Radio.route) {
      // TODO: Implement RadioScreen
      PlaceholderScreen("Radio Stations")
    }

    composable(Screen.Settings.route) {
      // TODO: Implement SettingsScreen (backport from development)
      PlaceholderScreen("Settings")
    }

    composable(Screen.Help.route) {
      // TODO: Implement HelpScreen
      PlaceholderScreen("Help & Feedback")
    }

    // Detail screens with arguments
    composable(
      route = Screen.AlbumTracks.ROUTE,
      arguments = listOf(
        navArgument("albumId") { type = NavType.LongType }
      )
    ) { backStackEntry ->
      val albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L
      // TODO: Implement AlbumTracksScreen
      PlaceholderScreen("Album Tracks (ID: $albumId)")
    }

    composable(
      route = Screen.ArtistAlbums.ROUTE,
      arguments = listOf(
        navArgument("artistId") { type = NavType.LongType }
      )
    ) { backStackEntry ->
      val artistId = backStackEntry.arguments?.getLong("artistId") ?: 0L
      // TODO: Implement ArtistAlbumsScreen
      PlaceholderScreen("Artist Albums (ID: $artistId)")
    }

    composable(
      route = Screen.GenreArtists.ROUTE,
      arguments = listOf(
        navArgument("genreId") { type = NavType.LongType }
      )
    ) { backStackEntry ->
      val genreId = backStackEntry.arguments?.getLong("genreId") ?: 0L
      // TODO: Implement GenreArtistsScreen
      PlaceholderScreen("Genre Artists (ID: $genreId)")
    }

    composable(
      route = Screen.PlaylistTracks.ROUTE,
      arguments = listOf(
        navArgument("playlistId") { type = NavType.LongType }
      )
    ) { backStackEntry ->
      val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
      // TODO: Implement PlaylistTracksScreen
      PlaceholderScreen("Playlist Tracks (ID: $playlistId)")
    }

    composable(Screen.NowPlayingList.route) {
      // TODO: Implement NowPlayingListScreen
      PlaceholderScreen("Now Playing List")
    }

    composable(Screen.Lyrics.route) {
      // TODO: Implement LyricsScreen
      PlaceholderScreen("Lyrics")
    }

    composable(Screen.ConnectionManager.route) {
      // TODO: Implement ConnectionManagerScreen
      PlaceholderScreen("Connection Manager")
    }
  }
}

/**
 * Type-safe navigation destinations using sealed classes.
 * This ensures compile-time safety for navigation and makes refactoring easier.
 */
sealed class Screen(val route: String) {
  // Main screens (drawer items)
  data object Home : Screen("home")
  data object Library : Screen("library")
  data object Playlists : Screen("playlists")
  data object Radio : Screen("radio")
  data object Settings : Screen("settings")
  data object Help : Screen("help")

  // Screens without arguments
  data object NowPlayingList : Screen("now_playing_list")
  data object Lyrics : Screen("lyrics")
  data object ConnectionManager : Screen("connection_manager")

  // Detail screens with arguments (using companion objects for route templates)
  data class AlbumTracks(val albumId: Long) : Screen("album_tracks/$albumId") {
    companion object {
      const val ROUTE = "album_tracks/{albumId}"
    }
  }

  data class ArtistAlbums(val artistId: Long) : Screen("artist_albums/$artistId") {
    companion object {
      const val ROUTE = "artist_albums/{artistId}"
    }
  }

  data class GenreArtists(val genreId: Long) : Screen("genre_artists/$genreId") {
    companion object {
      const val ROUTE = "genre_artists/{genreId}"
    }
  }

  data class PlaylistTracks(val playlistId: Long) : Screen("playlist_tracks/$playlistId") {
    companion object {
      const val ROUTE = "playlist_tracks/{playlistId}"
    }
  }
}

/**
 * Drawer navigation items for easy iteration.
 */
val drawerScreens = listOf(
  Screen.Home,
  Screen.Library,
  Screen.Playlists,
  Screen.Radio,
  Screen.Settings,
  Screen.Help
)

/**
 * Temporary placeholder screen for unimplemented screens.
 * Will be replaced with actual implementations in subsequent phases.
 */
@Composable
private fun PlaceholderScreen(screenName: String) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "TODO: $screenName",
      style = MaterialTheme.typography.headlineMedium
    )
  }
}
