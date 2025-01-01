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

class GenreEntryAdapter(
  context: Activity,
) : RecyclerView.Adapter<GenreEntryAdapter.ViewHolder>() {
  private var data: FlowCursorList<Genre>? = null
  private var listener: MenuItemSelectedListener? = null
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  fun setMenuItemSelectedListener(listener: MenuItemSelectedListener) {
    this.listener = listener
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val view = inflater.inflate(R.layout.listitem_single, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    val genre = data?.getItem(position.toLong())

    genre?.let {
      holder.title.text = if (it.genre.isNullOrBlank()) holder.empty else genre.genre
      holder.indicator.setOnClickListener { createPopup(it, genre) }
      holder.itemView.setOnClickListener { listener?.onItemClicked(genre) }
    }
  }

  private fun createPopup(
    it: View,
    genre: Genre,
  ) {
    val popupMenu = PopupMenu(it.context, it)
    popupMenu.inflate(R.menu.popup_genre)
    popupMenu.setOnMenuItemClickListener { menuItem ->
      return@setOnMenuItemClickListener listener?.onMenuItemSelected(menuItem, genre) ?: false
    }
    popupMenu.show()
  }

  /**
   * Returns the total number of items in the data set hold by the adapter.

   * @return The total number of items in this adapter.
   */
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
      genre: Genre,
    ): Boolean

    fun onItemClicked(genre: Genre)
  }

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(R.id.line_one)
    val indicator: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
    val empty: String = itemView.context.getString(R.string.empty)
  }

  fun update(cursor: FlowCursorList<Genre>) {
    data = cursor
    notifyDataSetChanged()
  }
}
