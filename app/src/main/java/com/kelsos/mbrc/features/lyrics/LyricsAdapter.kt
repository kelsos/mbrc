package com.kelsos.mbrc.features.lyrics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R

class LyricsAdapter(
  private var data: List<String> = emptyList(),
) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {
  /**
   * Called when RecyclerView needs a new [ViewHolder] of the given type to represent
   * an item.
   *
   *
   * This new ViewHolder should be constructed with a new View that can represent the items
   * of the given type. You can either create a new View manually or inflate it from an XML
   * layout file.
   *
   *
   * The new ViewHolder will be used to display items of the adapter using
   * [.onBindViewHolder]. Since it will be re-used to display different
   * items in the data set, it is a good idea to cache references to sub views of the View to
   * avoid unnecessary [android.view.View.findViewById] calls.

   * @param parent The ViewGroup into which the new View will be added after it is bound to
   * * an adapter position.
   * *
   * @param viewType The view type of the new View.
   * *
   * @return A new ViewHolder that holds a View of the given view type.
   * *
   * @see .getItemViewType
   * @see .onBindViewHolder
   */
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.ui_list_lyrics_item, parent, false)
    return ViewHolder(view)
  }

  /**
   * Called by RecyclerView to display the data at the specified position. This method
   * should update the contents of the [RecyclerView.ViewHolder.itemView] to reflect the item at
   * the given position.
   *
   *
   * Note that unlike [android.widget.ListView], RecyclerView will not call this
   * method again if the position of the item changes in the data set unless the item itself
   * is invalidated or the new position cannot be determined. For this reason, you should only
   * use the `position` parameter while acquiring the related data item inside this
   * method and should not keep a copy of it. If you need the position of an item later on
   * (e.g. in a click listener), use [RecyclerView.ViewHolder.getPosition] which will have the
   * updated position.

   * @param holder The ViewHolder which should be updated to represent the contents of the
   * * item at the given position in the data set.
   * *
   * @param position The position of the item within the adapter's data set.
   */
  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
  ) {
    val str = data[position]
    holder.title.text = str
  }

  fun updateLyrics(lyrics: List<String>) {
    this.data = lyrics
    notifyDataSetChanged()
  }

  /**
   * Returns the total number of items in the data set hold by the adapter.

   * @return The total number of items in this adapter.
   */
  override fun getItemCount(): Int = data.size

  class ViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(android.R.id.text1)
  }

  fun clear() {
    if (data.isNotEmpty()) {
      data = emptyList()
    }
  }
}
