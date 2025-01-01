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
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): ViewHolder {
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.ui_list_lyrics_item, parent, false)
    return ViewHolder(view)
  }

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
