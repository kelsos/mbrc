package com.kelsos.mbrc.features.library.artists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.MenuItemSelectedListener
import com.kelsos.mbrc.features.library.popupMenu

class ArtistEntryAdapter : PagingDataAdapter<Artist, ArtistEntryAdapter.ViewHolder>(DIFF_CALLBACK) {
  private var listener: MenuItemSelectedListener<Artist>? = null

  fun setMenuItemSelectedListener(listener: MenuItemSelectedListener<Artist>) {
    this.listener = listener
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val inflater: LayoutInflater = LayoutInflater.from(parent.context)
    val view = inflater.inflate(R.layout.listitem_single, parent, false)
    val holder = ViewHolder(view)

    holder.indicator.setOnClickListener {
      it.popupMenu(R.menu.popup_artist) {
        val position = holder.bindingAdapterPosition
        getItem(position)?.let { artist ->
          listener?.onAction(artist, it)
          true
        }
      }
    }

    holder.itemView.setOnClickListener {
      val position = holder.bindingAdapterPosition
      getItem(position)?.let { artist ->
        listener?.onAction(artist)
      }
    }
    return holder
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    getItem(position)?.let {
      holder.title.text = it.artist.ifBlank { holder.empty }
    }
  }

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.line_one)
    val indicator: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
    val empty: String = itemView.context.getString(R.string.empty)
  }

  companion object {
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<Artist>() {
        override fun areItemsTheSame(
          oldItem: Artist,
          newItem: Artist,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
          oldItem: Artist,
          newItem: Artist,
        ): Boolean = oldItem == newItem
      }
  }
}
