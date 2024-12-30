package com.kelsos.mbrc.helper

import android.content.Context
import android.content.Intent
import android.view.MenuItem
import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Album
import com.kelsos.mbrc.data.library.Artist
import com.kelsos.mbrc.data.library.Genre
import com.kelsos.mbrc.mappers.AlbumMapper
import com.kelsos.mbrc.ui.navigation.library.album_tracks.AlbumTracksActivity
import com.kelsos.mbrc.ui.navigation.library.artist_albums.ArtistAlbumsActivity
import com.kelsos.mbrc.ui.navigation.library.genre_artists.GenreArtistsActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PopupActionHandler
@Inject
constructor() {

  @Queue.Action
  fun albumSelected(menuItem: MenuItem, entry: Album, context: Context): String {
    if (menuItem.itemId == R.id.popup_album_tracks) {
      openProfile(entry, context)
      return Queue.PROFILE
    }

    return when (menuItem.itemId) {
      R.id.popup_album_queue_next -> Queue.NEXT
      R.id.popup_album_queue_last -> Queue.LAST
      R.id.popup_album_play -> Queue.NOW
      else -> Queue.NOW
    }
  }

  @Queue.Action
  fun artistSelected(menuItem: MenuItem, entry: Artist, context: Context): String {
    if (menuItem.itemId == R.id.popup_artist_album) {
      openProfile(entry, context)
      return Queue.PROFILE
    }

    return when (menuItem.itemId) {
      R.id.popup_artist_queue_next -> Queue.NEXT
      R.id.popup_artist_queue_last -> Queue.LAST
      R.id.popup_artist_play -> Queue.NOW
      else -> Queue.NOW
    }
  }

  @Queue.Action
  fun genreSelected(menuItem: MenuItem, entry: Genre, context: Context): String {
    if (R.id.popup_genre_artists == menuItem.itemId) {
      openProfile(entry, context)
      return Queue.PROFILE
    }

    return when (menuItem.itemId) {
      R.id.popup_genre_queue_next -> Queue.NEXT
      R.id.popup_genre_queue_last -> Queue.LAST
      R.id.popup_genre_play -> Queue.NOW
      else -> Queue.NOW
    }
  }

  @Queue.Action
  fun trackSelected(menuItem: MenuItem): String =
    when (menuItem.itemId) {
      R.id.popup_track_queue_next -> Queue.NEXT
      R.id.popup_track_queue_last -> Queue.LAST
      R.id.popup_track_play -> Queue.NOW
      R.id.popup_track_play_queue_all -> Queue.ADD_ALL
      R.id.popup_track_play_artist -> Queue.PLAY_ARTIST
      R.id.popup_track_play_album -> Queue.PLAY_ALBUM
      else -> Queue.NOW
    }

  fun albumSelected(album: Album, context: Context) {
    openProfile(album, context)
  }

  fun artistSelected(artist: Artist, context: Context) {
    openProfile(artist, context)
  }

  fun genreSelected(genre: Genre, context: Context) {
    openProfile(genre, context)
  }

  private fun openProfile(artist: Artist, context: Context) {
    val intent = Intent(context, ArtistAlbumsActivity::class.java)
    intent.putExtra(ArtistAlbumsActivity.ARTIST_NAME, artist.artist)
    context.startActivity(intent)
  }

  private fun openProfile(album: Album, context: Context) {
    val mapper = AlbumMapper()
    val intent = Intent(context, AlbumTracksActivity::class.java)
    intent.putExtra(AlbumTracksActivity.ALBUM, mapper.map(album))
    context.startActivity(intent)
  }

  private fun openProfile(genre: Genre, context: Context) {
    val intent = Intent(context, GenreArtistsActivity::class.java)
    intent.putExtra(GenreArtistsActivity.GENRE_NAME, genre.genre)
    context.startActivity(intent)
  }
}
