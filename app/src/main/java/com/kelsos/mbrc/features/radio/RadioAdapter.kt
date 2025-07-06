package com.kelsos.mbrc.features.radio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R

class RadioAdapter : PagingDataAdapter<RadioStation, RadioAdapter.ViewHolder>(DIFF_CALLBACK) {
  private var radioPressedListener: OnRadioPressedListener? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val view = inflater.inflate(R.layout.item_single, parent, false)
    val viewHolder = ViewHolder(view)

    viewHolder.itemView.setOnClickListener {
      val adapterPosition = viewHolder.bindingAdapterPosition
      if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
      val path = getItem(adapterPosition)?.url
      if (path != null) {
        radioPressedListener?.onRadioPressed(path)
      }
    }
    return viewHolder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val radio = getItem(position)
    holder.name.text = radio?.name.orEmpty()
    holder.context.visibility = View.GONE
  }

  fun setOnRadioPressedListener(onRadioPressedListener: OnRadioPressedListener?) {
    this.radioPressedListener = onRadioPressedListener
  }

  fun interface OnRadioPressedListener {
    fun onRadioPressed(path: String)
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: TextView = itemView.findViewById(R.id.line_one)
    val context: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
  }

  companion object {
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<RadioStation>() {
        override fun areItemsTheSame(oldItem: RadioStation, newItem: RadioStation): Boolean =
          oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RadioStation, newItem: RadioStation): Boolean =
          oldItem == newItem
      }
  }
}
