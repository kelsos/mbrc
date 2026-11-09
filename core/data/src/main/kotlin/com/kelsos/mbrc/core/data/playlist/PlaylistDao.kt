package com.kelsos.mbrc.core.data.playlist

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Unified browser item that can represent either a folder or a playlist.
 * Used for the combined paged query.
 *
 * @property itemType Item type: [ITEM_TYPE_FOLDER] or [ITEM_TYPE_PLAYLIST]
 * @property name Display name
 * @property path Folder path (for folders) or playlist url (for playlists)
 * @property id Playlist id or 0 for folder
 */
data class PlaylistBrowserItem(
  val itemType: String,
  val name: String,
  val path: String,
  val id: Long
) {
  val isFolder: Boolean get() = itemType == ITEM_TYPE_FOLDER
  val isPlaylist: Boolean get() = itemType == ITEM_TYPE_PLAYLIST

  /** The row's identity. Every folder has an [id] of 0, so a folder is identified by its [path]. */
  val key: String get() = if (isFolder) "folder:$path" else "playlist:$id"

  companion object {
    const val ITEM_TYPE_FOLDER = "folder"
    const val ITEM_TYPE_PLAYLIST = "playlist"
  }
}

@Dao
interface PlaylistDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertAll(list: List<PlaylistEntity>)

  @Query("select * from playlists")
  fun getAll(): PagingSource<Int, PlaylistEntity>

  @Query("select * from playlists")
  fun all(): List<PlaylistEntity>

  @Query("select * from playlists where name like '%'|| :term ||'%'")
  fun search(term: String): PagingSource<Int, PlaylistEntity>

  @Query("select count(*) from playlists")
  fun count(): Long

  @Query("delete from playlists where date_added != :added")
  fun removePreviousEntries(added: Long)

  @Query("select * from playlists where id = :id")
  fun getById(id: Long): PlaylistEntity?

  /**
   * Get all items (folders + playlists) at root level.
   * Folders are derived from playlist names containing backslash.
   * Returns a paged source with folders first, then playlists, both sorted by name.
   */
  @Query(
    """
    SELECT
      'folder' as itemType,
      SUBSTR(name, 1, INSTR(name, '\') - 1) as name,
      SUBSTR(name, 1, INSTR(name, '\') - 1) as path,
      0 as id
    FROM playlists
    WHERE INSTR(name, '\') > 0
    GROUP BY SUBSTR(name, 1, INSTR(name, '\') - 1)

    UNION ALL

    SELECT
      'playlist' as itemType,
      name,
      url as path,
      id
    FROM playlists
    WHERE INSTR(name, '\') = 0

    ORDER BY itemType ASC, name COLLATE NOCASE
    """
  )
  fun getBrowserItemsAtRoot(): PagingSource<Int, PlaylistBrowserItem>

  /**
   * Get all items (folders + playlists) at a specific path.
   * The path parameter should end without a backslash (e.g., "tracks" or "tracks\rock").
   */
  @Query(
    """
    SELECT
      'folder' as itemType,
      SUBSTR(name, LENGTH(:path) + 2, INSTR(SUBSTR(name, LENGTH(:path) + 2), '\') - 1) as name,
      :path || '\' || SUBSTR(name, LENGTH(:path) + 2, INSTR(SUBSTR(name, LENGTH(:path) + 2), '\') - 1) as path,
      0 as id
    FROM playlists
    WHERE name LIKE :path || '\%'
      AND INSTR(SUBSTR(name, LENGTH(:path) + 2), '\') > 0
    GROUP BY SUBSTR(name, LENGTH(:path) + 2, INSTR(SUBSTR(name, LENGTH(:path) + 2), '\') - 1)

    UNION ALL

    SELECT
      'playlist' as itemType,
      SUBSTR(name, LENGTH(:path) + 2) as name,
      url as path,
      id
    FROM playlists
    WHERE name LIKE :path || '\%'
      AND INSTR(SUBSTR(name, LENGTH(:path) + 2), '\') = 0

    ORDER BY itemType ASC, name COLLATE NOCASE
    """
  )
  fun getBrowserItemsAtPath(path: String): PagingSource<Int, PlaylistBrowserItem>
}
