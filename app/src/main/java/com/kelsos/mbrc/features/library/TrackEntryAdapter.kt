package com.kelsos.mbrc.features.library

import android.app.Activity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import com.kelsos.mbrc.R
import com.raizlabs.android.dbflow.list.FlowCursorList
import java.io.File

class TrackEntryAdapter(
  context: Activity,
) : RecyclerView.Adapter<TrackEntryAdapter.ViewHolder>() {
  private var data: FlowCursorList<Track>? = null
  private var listener: MenuItemSelectedListener? = null
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private val cache = File(context.cacheDir, "covers")
  private var coverMode: Boolean = false

  fun setCoverMode(enabled: Boolean) {
    coverMode = enabled
  }

  fun setMenuItemSelectedListener(listener: MenuItemSelectedListener) {
    this.listener = listener
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val view = inflater.inflate(R.layout.ui_list_dual, parent, false)
    val holder = ViewHolder(view)
    holder.indicator.setOnClickListener { createPopup(it, holder) }

    holder.itemView.setOnClickListener {
      val position = holder.bindingAdapterPosition.toLong()
      val track = data?.getItem(position) ?: return@setOnClickListener
      listener?.onItemClicked(track)
    }
    return holder
  }

  private fun createPopup(
    it: View,
    holder: ViewHolder,
  ) {
    val popupMenu = PopupMenu(it.context, it)
    popupMenu.inflate(R.menu.popup_track)
    popupMenu.setOnMenuItemClickListener { menuItem ->
      val position = holder.bindingAdapterPosition.toLong()
      val track = data?.getItem(position) ?: return@setOnMenuItemClickListener false
      listener?.onMenuItemSelected(menuItem, track)
      true
    }
    popupMenu.show()
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    val entry = data?.getItem(position.toLong()) ?: return
    holder.title.text = entry.title
    val artist = entry.artist
    holder.artist.text = if (artist.isNullOrBlank()) holder.unknownArtist else artist
    holder.image.isGone = !coverMode
    holder.image.load(File(cache, entry.key())) {
      crossfade(false)
      placeholder(R.drawable.ic_image_no_cover)
      error(R.drawable.ic_image_no_cover)
      size(
        holder.itemView.context.resources
          .getDimensionPixelSize(R.dimen.list_album_size),
      )
      scale(Scale.FILL)
    }
  }

  override fun getItemCount(): Int {
    val count = data?.count?.toInt()
    return count ?: 0
  }

  fun refresh() {
    data?.refresh()
    notifyDataSetChanged()
  }

  interface MenuItemSelectedListener {
    fun onMenuItemSelected(
      menuItem: MenuItem,
      track: Track,
    )

    fun onItemClicked(track: Track)
  }

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val artist: TextView = itemView.findViewById(R.id.line_two)
    val title: TextView = itemView.findViewById(R.id.line_one)
    val indicator: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
    val unknownArtist: String = itemView.context.getString(R.string.unknown_artist)
    val image: ImageView = itemView.findViewById(R.id.cover)
  }

  fun update(cursor: FlowCursorList<Track>) {
    data = cursor
    notifyDataSetChanged()
  }
}
