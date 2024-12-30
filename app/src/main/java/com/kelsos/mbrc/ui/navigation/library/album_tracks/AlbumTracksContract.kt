package com.kelsos.mbrc.ui.navigation.library.album_tracks

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Track
import com.kelsos.mbrc.domain.AlbumInfo
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface AlbumTracksView : BaseView {
  fun update(cursor: FlowCursorList<Track>)
  fun queue(success: Boolean, tracks: Int)
}

interface AlbumTracksPresenter : Presenter<AlbumTracksView> {
  fun load(album: AlbumInfo)
  fun queue(entry: Track, @Queue.Action action: String? = null)
  fun queueAlbum(artist: String, album: String)
}

