package com.kelsos.mbrc.features.library.albums

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.SquareImageView
import com.kelsos.mbrc.features.library.MenuItemSelectedListener
import com.kelsos.mbrc.features.library.popupMenu
import timber.log.Timber
import java.io.File

class AlbumEntryAdapter : PagingDataAdapter<Album, AlbumEntryAdapter.ViewHolder>(DIFF_CALLBACK) {
  private var listener: MenuItemSelectedListener<Album>? = null

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val inflater: LayoutInflater = LayoutInflater.from(parent.context)
    val view = inflater.inflate(R.layout.item_album, parent, false)
    val holder = ViewHolder(view)
    holder.indicator.setOnClickListener {
      it.popupMenu(R.menu.popup_album) {
        val position = holder.bindingAdapterPosition
        getItem(position)?.let { album ->
          listener?.onAction(album, it)
        }
      }
    }

    holder.itemView.setOnClickListener {
      val position = holder.bindingAdapterPosition
      getItem(position)?.let { album ->
        listener?.onAction(album)
      }
    }
    return holder
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    val context = holder.itemView.context
    val cache = File(context.cacheDir, "covers")
    val item = getItem(position) ?: return
    val (_, artist, album) = item
    holder.album.text = album.ifBlank { holder.emptyAlbum }
    holder.artist.text = artist.ifBlank { holder.unknownArtist }

    Timber.v(item.key())

    holder.image.load(File(cache, item.key())) {
      crossfade(false)
      placeholder(R.drawable.ic_image_no_cover)
      error(R.drawable.ic_image_no_cover)
      size(context.resources.getDimensionPixelSize(R.dimen.list_album_size))
      scale(Scale.FILL)
    }
  }

  fun setMenuItemSelectedListener(listener: MenuItemSelectedListener<Album>) {
    this.listener = listener
  }

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val artist: TextView = itemView.findViewById(R.id.line_two)
    val album: TextView = itemView.findViewById(R.id.line_one)
    val image: SquareImageView = itemView.findViewById(R.id.cover)
    val indicator: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)

    val unknownArtist: String = itemView.context.getString(R.string.unknown_artist)
    val emptyAlbum: String = itemView.context.getString(R.string.non_album_tracks)

    init {
      image.isGone = false
    }
  }

  companion object {
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<Album>() {
        override fun areItemsTheSame(
          oldItem: Album,
          newItem: Album,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
          oldItem: Album,
          newItem: Album,
        ): Boolean = oldItem == newItem
      }
  }
}
