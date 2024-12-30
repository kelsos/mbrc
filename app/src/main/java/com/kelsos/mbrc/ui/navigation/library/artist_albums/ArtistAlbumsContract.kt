package com.kelsos.mbrc.ui.navigation.library.artist_albums

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Album
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface ArtistAlbumsView : BaseView {
  fun update(albums: FlowCursorList<Album>)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}

interface ArtistAlbumsPresenter : Presenter<ArtistAlbumsView> {
  fun load(artist: String)

  fun queue(
    @Queue.Action action: String,
    album: Album,
  )
}
