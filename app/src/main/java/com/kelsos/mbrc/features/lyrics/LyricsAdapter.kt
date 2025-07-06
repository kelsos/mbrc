package com.kelsos.mbrc.features.lyrics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R

class LyricsAdapter : ListAdapter<String, LyricsAdapter.ViewHolder>(DIFF_CALLBACK) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val view = inflater.inflate(R.layout.item_lyrics, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.title.text = getItem(position)
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(android.R.id.text1)
  }

  companion object {
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
          oldItem == newItem
      }
  }
}
