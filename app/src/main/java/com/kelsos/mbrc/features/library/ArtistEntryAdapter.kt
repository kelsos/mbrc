package com.kelsos.mbrc.features.library

import android.app.Activity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R
import com.raizlabs.android.dbflow.list.FlowCursorList

class ArtistEntryAdapter(
  context: Activity,
) : RecyclerView.Adapter<ArtistEntryAdapter.ViewHolder>() {
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data: FlowCursorList<Artist>? = null
  private var listener: MenuItemSelectedListener? = null

  fun setMenuItemSelectedListener(listener: MenuItemSelectedListener) {
    this.listener = listener
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val view = inflater.inflate(R.layout.listitem_single, parent, false)
    val holder = ViewHolder(view)

    holder.indicator.setOnClickListener {
      val popupMenu = PopupMenu(it.context, it)
      popupMenu.inflate(R.menu.popup_artist)
      popupMenu.setOnMenuItemClickListener { menuItem ->
        val position = holder.bindingAdapterPosition.toLong()
        val artist = data?.getItem(position) ?: return@setOnMenuItemClickListener false
        listener?.onMenuItemSelected(menuItem, artist)
        true
      }
      popupMenu.show()
    }

    holder.itemView.setOnClickListener {
      val position = holder.bindingAdapterPosition.toLong()
      val artist = data?.getItem(position) ?: return@setOnClickListener
      listener?.onItemClicked(artist)
    }
    return holder
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    val artist = data?.getItem(position.toLong())

    artist?.let {
      holder.title.text =
        if (it.artist.isNullOrBlank()) {
          holder.empty
        } else {
          it.artist
        }
    }
  }

  override fun getItemCount(): Int = data?.count?.toInt() ?: 0

  fun refresh() {
    data?.refresh()
    notifyDataSetChanged()
  }

  interface MenuItemSelectedListener {
    fun onMenuItemSelected(
      menuItem: MenuItem,
      artist: Artist,
    )

    fun onItemClicked(artist: Artist)
  }

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.line_one)
    val indicator: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
    val empty: String = itemView.context.getString(R.string.empty)
  }

  fun update(data: FlowCursorList<Artist>) {
    this.data = data
    notifyDataSetChanged()
  }
}
