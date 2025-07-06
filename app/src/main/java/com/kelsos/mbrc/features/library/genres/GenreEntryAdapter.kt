package com.kelsos.mbrc.features.library.genres

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

class GenreEntryAdapter : PagingDataAdapter<Genre, GenreEntryAdapter.ViewHolder>(DIFF_CALLBACK) {
  private var listener: MenuItemSelectedListener<Genre>? = null

  fun setMenuItemSelectedListener(listener: MenuItemSelectedListener<Genre>) {
    this.listener = listener
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater: LayoutInflater = LayoutInflater.from(parent.context)
    val view = inflater.inflate(R.layout.item_single, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val genre = getItem(position)

    genre?.let {
      holder.title.text = if (it.genre.isBlank()) holder.empty else genre.genre
      holder.indicator.setOnClickListener { menu ->
        menu.popupMenu(R.menu.popup_genre) { id ->
          getItem(holder.bindingAdapterPosition)?.let { genre ->
            listener?.onAction(genre, id)
            true
          }
        }
      }
      holder.itemView.setOnClickListener { listener?.onAction(genre) }
    }
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.line_one)
    val indicator: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
    val empty: String = itemView.context.getString(R.string.empty)
  }

  companion object {
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<Genre>() {
        override fun areItemsTheSame(oldItem: Genre, newItem: Genre): Boolean =
          oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Genre, newItem: Genre): Boolean =
          oldItem == newItem
      }
  }
}
