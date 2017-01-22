package com.kelsos.mbrc.helper

import android.content.Context
import android.content.Intent
import android.view.MenuItem
import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.annotations.Queue.QueueType
import com.kelsos.mbrc.data.library.Album
import com.kelsos.mbrc.data.library.Artist
import com.kelsos.mbrc.data.library.Genre
import com.kelsos.mbrc.data.library.Track
import com.kelsos.mbrc.mappers.AlbumMapper
import com.kelsos.mbrc.repository.TrackRepository
import com.kelsos.mbrc.services.QueueService
import com.kelsos.mbrc.ui.navigation.library.album_tracks.AlbumTracksActivity
import com.kelsos.mbrc.ui.navigation.library.artist_albums.ArtistAlbumsActivity
import com.kelsos.mbrc.ui.navigation.library.genre_artists.GenreArtistsActivity
import rx.Scheduler
import rx.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PopupActionHandler
@Inject
constructor(private val settings: BasicSettingsHelper,
            @Named("io") private val ioScheduler: Scheduler,
            private val trackRepository: TrackRepository,
            private val queueService: QueueService) {

  fun albumSelected(menuItem: MenuItem, entry: Album, context: Context) {

    if (menuItem.itemId == R.id.popup_album_tracks) {
      openProfile(entry, context)
      return
    }


    val type = when (menuItem.itemId) {
      R.id.popup_album_queue_next -> Queue.NEXT
      R.id.popup_album_queue_last -> Queue.LAST
      R.id.popup_album_play -> Queue.NOW
      else -> Queue.NOW
    }

    queueAlbum(entry, type)
  }

  private fun queueAlbum(entry: Album, @QueueType type: String) {
    trackRepository.getAlbumTrackPaths(entry.album!!, entry.artist!!).flatMap {
      queueService.queue(type, it)
    }.subscribeOn(ioScheduler).subscribe({

    }) {
      Timber.v(it, "Failed to queue")
    }
  }

  fun artistSelected(menuItem: MenuItem, entry: Artist, context: Context) {

    if (menuItem.itemId == R.id.popup_artist_album) {
      openProfile(entry, context)
      return
    }

    val type = when (menuItem.itemId) {
      R.id.popup_artist_queue_next -> Queue.NEXT
      R.id.popup_artist_queue_last -> Queue.LAST
      R.id.popup_artist_play -> Queue.NOW
      else -> Queue.NOW
    }

    queueArtist(entry, type)
  }

  private fun queueArtist(entry: Artist, type: String) {
    trackRepository.getArtistTrackPaths(artist = entry.artist!!).flatMap {
      queueService.queue(type, it)
    }.subscribeOn(ioScheduler).subscribe({

    }) {
      Timber.v(it, "Failed to queue")
    }
  }

  fun genreSelected(menuItem: MenuItem, entry: Genre, context: Context) {

    if (R.id.popup_genre_artists == menuItem.itemId) {
      openProfile(entry, context)
      return
    }


    val type = when (menuItem.itemId) {
      R.id.popup_genre_queue_next -> Queue.NEXT
      R.id.popup_genre_queue_last -> Queue.LAST
      R.id.popup_genre_play -> Queue.NOW
      else -> Queue.NOW
    }

    queueGenre(entry, type)
  }

  private fun queueGenre(entry: Genre, type: String) {
    trackRepository.getGenreTrackPaths(genre = entry.genre!!).flatMap {
      queueService.queue(type, it)
    }.subscribeOn(ioScheduler).subscribe({

    }) {
      Timber.v(it, "Failed to queue")
    }
  }

  //todo album detection -> queue album tracks
  fun trackSelected(menuItem: MenuItem, entry: Track, album: Boolean = false) {
    val type = when (menuItem.itemId) {
      R.id.popup_track_queue_next -> Queue.NEXT
      R.id.popup_track_queue_last -> Queue.LAST
      R.id.popup_track_play -> Queue.NOW
      R.id.popup_track_play_queue_all -> Queue.ADD_ALL
      else -> Queue.NOW
    }

    queueTrack(entry, type, album)
  }

  private fun queueTrack(entry: Track, @QueueType type: String, album: Boolean = false) {

    val trackSource: Single<List<String>>
    val path:String?
    if (type == Queue.ADD_ALL) {
      if (album) {
        trackSource = trackRepository.getAlbumTrackPaths(entry.album!!, entry.albumArtist!!)
      } else {
        trackSource = trackRepository.getAllTrackPaths()
      }

      path = entry.src

    } else {
      trackSource = Single.fromCallable {
        val list = listOf(entry.src!!)
        return@fromCallable list
      }
      path = null
    }

    trackSource.flatMap { queueService.queue(type, it, path) }
        .subscribeOn(ioScheduler)
        .subscribe({ }) {
          Timber.v(it, "Failed to queue")
        }
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

  fun trackSelected(track: Track, album: Boolean = false) {
    queueTrack(track, settings.defaultAction, album)
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
