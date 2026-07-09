package com.kelsos.mbrc.feature.library

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.LibraryTransactionRunner
import com.kelsos.mbrc.core.data.library.track.TrackEntity
import com.kelsos.mbrc.feature.library.domain.LibraryDerivationUseCase
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDerivationUseCaseTest {
  private lateinit var db: Database
  private lateinit var derivation: LibraryDerivationUseCase

  @Before
  fun setUp() {
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      Database::class.java
    ).allowMainThreadQueries().build()

    derivation = LibraryDerivationUseCase(
      trackDao = db.trackDao(),
      genreDao = db.genreDao(),
      artistDao = db.artistDao(),
      albumDao = db.albumDao(),
      trackGenreDao = db.trackGenreDao(),
      trackArtistDao = db.trackArtistDao(),
      transactionRunner = LibraryTransactionRunner(db)
    )
  }

  @After
  fun tearDown() {
    db.close()
  }

  private fun track(
    src: String,
    artist: String,
    albumArtist: String,
    album: String,
    genre: String
  ) = TrackEntity(
    artist = artist,
    title = src,
    src = src,
    trackno = 1,
    disc = 1,
    albumArtist = albumArtist,
    album = album,
    genre = genre,
    year = "2005",
    sortableYear = "2005",
    dateAdded = 1
  )

  private fun seed() {
    db.trackDao().insertAll(
      listOf(
        // multi-value genre, single artist
        track(
          "vinland.mp3",
          "Leaves' Eyes",
          "Leaves' Eyes",
          "Vinland Saga",
          "Gothic Metal; Power Metal; Metal"
        ),
        // multi-value artist; album artist is single-value, as MusicBee defines it
        track(
          "split.mp3",
          "Leaves' Eyes;Metalium",
          "Leaves' Eyes",
          "Split",
          "Symphonic Metal"
        ),
        // single-value control that shares a genre with vinland
        track("keldian.mp3", "Keldian", "Keldian", "Journey", "Power Metal")
      )
    )
  }

  private suspend fun <T : Any> PagingSource<Int, T>.loadAll(): List<T> {
    val result = load(
      PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false)
    )
    return (result as PagingSource.LoadResult.Page).data
  }

  @Test
  fun `multi-value genre track is reachable under every split genre`() = runTest {
    seed()
    derivation.derive()

    val dao = db.trackDao()
    assertThat(dao.getGenreTrackPaths(listOf("Gothic Metal"))).containsExactly("vinland.mp3")
    assertThat(dao.getGenreTrackPaths(listOf("Metal"))).containsExactly("vinland.mp3")
    assertThat(dao.getGenreTrackPaths(listOf("Power Metal")))
      .containsExactly("vinland.mp3", "keldian.mp3")
  }

  @Test
  fun `multi-artist track is reachable under every split artist`() = runTest {
    seed()
    derivation.derive()

    val dao = db.trackDao()
    assertThat(dao.getArtistTrackPaths(listOf("Metalium"))).containsExactly("split.mp3")
    assertThat(dao.getArtistTrackPaths(listOf("Leaves' Eyes")))
      .containsExactly("vinland.mp3", "split.mp3")
    assertThat(dao.getArtistTrackPaths(listOf("Keldian"))).containsExactly("keldian.mp3")
  }

  @Test
  fun `only the semicolon separates artists, never ampersand or comma`() = runTest {
    // Real collaborations from a live library. MusicBee's separator is ';' alone:
    // "Wisin & Yandel", "Jowell & Randy" and "Me, Myself and I" are each a SINGLE
    // act whose name happens to contain '&' or ','. Splitting on those characters
    // would shatter them, and no delimiter can tell them apart from a genuine
    // collaboration - which is why the split has to come from the tag, not a parser.
    db.trackDao().insertAll(
      listOf(
        track(
          "amanece.mp3",
          "Jowell & Randy;Yandel;Gadiel",
          "Jowell & Randy",
          "El Momento",
          "Reggaeton"
        ),
        track(
          "sandungueo.mp3",
          "Gadiel;Wisin & Yandel;Yomo",
          "Gadiel",
          "5 estrellas the mixtape",
          "Reggaeton"
        ),
        track("mmi.mp3", "Me, Myself and I", "Me, Myself and I", "Solo", "Reggaeton")
      )
    )
    derivation.derive()

    assertThat(db.artistDao().all().map { it.artist })
      .containsExactly(
        "Jowell & Randy",
        "Yandel",
        "Gadiel",
        "Wisin & Yandel",
        "Yomo",
        "Me, Myself and I"
      )

    val dao = db.trackDao()
    // Each act reachable whole, with the duos intact rather than halved.
    assertThat(dao.getArtistTrackPaths(listOf("Wisin & Yandel"))).containsExactly("sandungueo.mp3")
    assertThat(dao.getArtistTrackPaths(listOf("Jowell & Randy"))).containsExactly("amanece.mp3")
    assertThat(dao.getArtistTrackPaths(listOf("Me, Myself and I"))).containsExactly("mmi.mp3")
    // Gadiel appears in both collaborations, under a different role in each.
    assertThat(dao.getArtistTrackPaths(listOf("Gadiel")))
      .containsExactly("amanece.mp3", "sandungueo.mp3")
    // A halved duo must not exist as an artist at all.
    assertThat(db.artistDao().all().map { it.artist }).containsNoneOf("Wisin", "Randy", "Me")
  }

  @Test
  fun `dimension tables are derived from split track tags`() = runTest {
    seed()
    derivation.derive()

    assertThat(db.genreDao().all().map { it.genre })
      .containsExactly("Gothic Metal", "Power Metal", "Metal", "Symphonic Metal")
    assertThat(db.artistDao().all().map { it.artist })
      .containsExactly("Leaves' Eyes", "Metalium", "Keldian")
    assertThat(db.albumDao().count()).isEqualTo(3)
  }

  @Test
  fun `genre to artists uses the artist role and follows split tags`() = runTest {
    seed()
    derivation.derive()

    val powerMetalId = db.genreDao().genres().first { it.genre == "Power Metal" }.id
    val artists = db.artistDao().getArtistByGenreAsc(powerMetalId).loadAll().map { it.artist }
    assertThat(artists).containsExactly("Keldian", "Leaves' Eyes")
  }

  @Test
  fun `album artists list uses the album-artist role and never splits`() = runTest {
    seed()
    derivation.derive()

    // "Metalium" is a track artist on split.mp3 but not its album artist, so it
    // must not appear here - this is what distinguishes the two junction roles.
    val albumArtists = db.artistDao().getAlbumArtistsAsc().loadAll().map { it.artist }
    assertThat(albumArtists).containsExactly("Keldian", "Leaves' Eyes")
  }

  @Test
  fun `a semicolon in album artist is one artist, not a separator`() = runTest {
    // MusicBee documents album artist as single-value: artist/composer/genre and
    // the splitter roles are the multi-value fields. A ';' here is bad tag data
    // (observed once in the wild as "宇多田ヒカル; ??????" mojibake), so it must be
    // taken verbatim rather than split into two browsable artists.
    db.trackDao().insertAll(
      listOf(track("heart.mp3", "Utada", "Utada; Bogus", "Heart Station", "Pop"))
    )
    derivation.derive()

    assertThat(db.artistDao().getAlbumArtistsAsc().loadAll().map { it.artist })
      .containsExactly("Utada; Bogus")
    assertThat(db.artistDao().all().map { it.artist })
      .containsExactly("Utada", "Utada; Bogus")
  }

  @Test
  fun `re-deriving prunes dimensions and junctions for removed tracks`() = runTest {
    seed()
    derivation.derive()

    // Remove the only Symphonic Metal / Metalium track and re-derive.
    db.trackDao().deletePaths(listOf("split.mp3"))
    derivation.derive()

    assertThat(db.genreDao().all().map { it.genre }).doesNotContain("Symphonic Metal")
    assertThat(db.artistDao().all().map { it.artist }).doesNotContain("Metalium")
    assertThat(db.trackDao().getArtistTrackPaths(listOf("Metalium"))).isEmpty()
  }

  @Test
  fun `ensureDerived backfills once then no-ops`() = runTest {
    seed()
    assertThat(db.trackArtistDao().count()).isEqualTo(0)

    derivation.ensureDerived()
    val afterFirst = db.trackArtistDao().count()
    assertThat(afterFirst).isGreaterThan(0)

    // Second call is a no-op: junctions already present.
    derivation.ensureDerived()
    assertThat(db.trackArtistDao().count()).isEqualTo(afterFirst)
  }

  @Test
  fun `albums are reachable under every split genre`() = runTest {
    seed()
    derivation.derive()

    val dao = db.albumDao()
    val genres = db.genreDao().genres().associate { it.genre to it.id }

    // Power Metal is one component of vinland's compound tag and the whole of
    // keldian's single-value tag: both albums must appear.
    assertThat(dao.getAlbumsByGenre(genres.getValue("Power Metal")).loadAll().map { it.album })
      .containsExactly("Journey", "Vinland Saga")
    // Gothic Metal and Metal only ever appear inside vinland's compound tag.
    assertThat(dao.getAlbumsByGenre(genres.getValue("Gothic Metal")).loadAll().map { it.album })
      .containsExactly("Vinland Saga")
    assertThat(dao.getAlbumsByGenre(genres.getValue("Metal")).loadAll().map { it.album })
      .containsExactly("Vinland Saga")
  }

  @Test
  fun `albums by genre honour every sort order`() = runTest {
    seed()
    derivation.derive()

    val dao = db.albumDao()
    val powerMetal = db.genreDao().genres().first { it.genre == "Power Metal" }.id

    // Journey/Keldian vs Vinland Saga/Leaves' Eyes: name and artist order agree
    // ascending, so each Desc variant must be the exact reverse of its Asc pair.
    assertThat(dao.getAlbumsByGenreByNameAsc(powerMetal).loadAll().map { it.album })
      .containsExactly("Journey", "Vinland Saga")
      .inOrder()
    assertThat(dao.getAlbumsByGenreByNameDesc(powerMetal).loadAll().map { it.album })
      .containsExactly("Vinland Saga", "Journey")
      .inOrder()
    assertThat(dao.getAlbumsByGenreByArtistAsc(powerMetal).loadAll().map { it.album })
      .containsExactly("Journey", "Vinland Saga")
      .inOrder()
    assertThat(dao.getAlbumsByGenreByArtistDesc(powerMetal).loadAll().map { it.album })
      .containsExactly("Vinland Saga", "Journey")
      .inOrder()
  }

  @Test
  fun `albums are reachable under every split artist`() = runTest {
    seed()
    derivation.derive()

    val dao = db.albumDao()

    // "Metalium" exists only as the second half of split.mp3's compound artist.
    assertThat(dao.getAlbumsByArtistByNameAsc("Metalium").loadAll().map { it.album })
      .containsExactly("Split")
    // "Leaves' Eyes" is a standalone tag on one album and a compound component on
    // the other; both must resolve.
    assertThat(dao.getAlbumsByArtistByNameAsc("Leaves' Eyes").loadAll().map { it.album })
      .containsExactly("Split", "Vinland Saga")
      .inOrder()
    assertThat(dao.getAlbumsByArtistByNameDesc("Leaves' Eyes").loadAll().map { it.album })
      .containsExactly("Vinland Saga", "Split")
      .inOrder()
  }

  @Test
  fun `a multi-artist album is not duplicated across its split artists`() = runTest {
    seed()
    derivation.derive()

    // split.mp3 carries the compound artist "Leaves' Eyes;Metalium": reaching its
    // album from either artist must yield the same single row, not one per artist.
    val fromMetalium = db.albumDao().getAlbumsByArtistByNameAsc("Metalium").loadAll()
    assertThat(fromMetalium).hasSize(1)
    assertThat(fromMetalium.single().artist).isEqualTo("Leaves' Eyes")
  }

  @Test
  fun `genre to artists and album artists honour descending order`() = runTest {
    seed()
    derivation.derive()

    val powerMetal = db.genreDao().genres().first { it.genre == "Power Metal" }.id
    assertThat(db.artistDao().getArtistByGenreDesc(powerMetal).loadAll().map { it.artist })
      .containsExactly("Leaves' Eyes", "Keldian")
      .inOrder()
    assertThat(db.artistDao().getAlbumArtistsDesc().loadAll().map { it.artist })
      .containsExactly("Leaves' Eyes", "Keldian")
      .inOrder()
  }

  @Test
  fun `single value tags are unaffected`() = runTest {
    db.trackDao().insertAll(
      listOf(track("only.mp3", "Solo", "Solo", "Alone", "Ambient"))
    )
    derivation.derive()

    assertThat(db.trackDao().getGenreTrackPaths(listOf("Ambient"))).containsExactly("only.mp3")
    assertThat(db.trackDao().getArtistTrackPaths(listOf("Solo"))).containsExactly("only.mp3")
    assertThat(db.genreDao().all().map { it.genre }).containsExactly("Ambient")
    assertThat(db.artistDao().all()).hasSize(1)
  }

  /**
   * Dimension names come from raw track tags now, not the plugin's canonical lists, and tags in the
   * wild differ in case. Both spellings must land on one row and both tracks must remain reachable
   * through it, which is what makes the NOCASE columns and the folded id lookup agree.
   */
  @Test
  fun `tags differing only by case are one dimension that both tracks reach`() = runTest {
    db.trackDao().insertAll(
      listOf(
        track("upper.mp3", "Keldian", "Keldian", "Journey", "Power Metal"),
        track("lower.mp3", "keldian", "keldian", "Journey", "power metal")
      )
    )
    derivation.derive()

    assertThat(db.genreDao().all()).hasSize(1)
    assertThat(db.artistDao().all()).hasSize(1)
    assertThat(db.trackDao().getGenreTrackPaths(listOf("Power Metal")))
      .containsExactly("upper.mp3", "lower.mp3")
    assertThat(db.trackDao().getGenreTrackPaths(listOf("power metal")))
      .containsExactly("upper.mp3", "lower.mp3")
    assertThat(db.trackDao().getArtistTrackPaths(listOf("KELDIAN")))
      .containsExactly("upper.mp3", "lower.mp3")
  }

  /**
   * Derivation rebuilds every dimension and junction from the whole track table on each sync, so
   * its cost grows with the library rather than with what changed. Real libraries reach tens of
   * thousands of tracks, where anything worse than linear stops being usable.
   *
   * The bound is deliberately loose: it is here to catch a quadratic regression, not to measure a
   * build machine. Correctness at this size is asserted alongside it, because a fast derivation
   * that drops rows would otherwise pass.
   *
   * Every track carries a compound genre and a compound artist, so the junctions are exercised at
   * full width rather than by single-value rows. The fixture gives each of [ARTISTS] album artists
   * exactly one album, so albums, which are identified by artist and title together, come to
   * [ARTISTS] as well.
   */
  @Test
  fun `derivation of a large library stays linear and complete`() = runTest {
    val tracks = (0 until LARGE_LIBRARY_TRACKS).map { i ->
      track(
        src = "track$i.mp3",
        artist = "Artist${i % ARTISTS};Guest${i % GUESTS}",
        albumArtist = "Artist${i % ARTISTS}",
        album = "Album${i % ARTISTS}",
        genre = "Genre${i % GENRES}; Metal"
      )
    }
    db.trackDao().insertAll(tracks)

    val elapsedMs = measureTimeMillis { derivation.derive() }

    assertThat(db.trackDao().count()).isEqualTo(LARGE_LIBRARY_TRACKS.toLong())
    assertThat(db.genreDao().all()).hasSize(GENRES + 1)
    assertThat(db.artistDao().all()).hasSize(ARTISTS + GUESTS)
    assertThat(db.albumDao().all()).hasSize(ARTISTS)
    assertThat(elapsedMs).isLessThan(LARGE_LIBRARY_BUDGET_MS)
  }

  companion object {
    /** Close to a real library; the library behind the original report is around 15k tracks. */
    private const val LARGE_LIBRARY_TRACKS = 15_000
    private const val ARTISTS = 900
    private const val GUESTS = 70

    /** Every track also carries "Metal", so the derived genre count is this plus one. */
    private const val GENRES = 40

    /** Loose enough to survive a slow CI box, tight enough that quadratic work blows through it. */
    private const val LARGE_LIBRARY_BUDGET_MS = 60_000L
  }
}
