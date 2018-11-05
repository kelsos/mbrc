package com.kelsos.mbrc.ui.navigation.radio

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.kelsos.mbrc.R
import com.kelsos.mbrc.content.radios.RadioStation
import kotterknife.bindView
import javax.inject.Inject

class RadioAdapter
@Inject constructor(context: Activity) : RecyclerView.Adapter<RadioAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data: List<RadioStation>? = null
  private var radioPressedListener: OnRadioPressedListener? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = inflater.inflate(R.layout.listitem_single, parent, false)
    val viewHolder = ViewHolder(view)

    viewHolder.itemView.setOnClickListener {
      val path = data?.get(viewHolder.adapterPosition)?.url
      path?.let {
        radioPressedListener?.onRadioPressed(it)
      }
    }
    return viewHolder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val radio = data?.get(holder.adapterPosition)
    radio?.let {
      holder.name.text = radio.name
    }
    holder.context.visibility = View.GONE
  }

  override fun getItemCount(): Int {
    return data?.size ?: 0
  }

  fun update(cursor: List<RadioStation>) {
    this.data = cursor
    notifyDataSetChanged()
  }

  fun setOnRadioPressedListener(onRadioPressedListener: OnRadioPressedListener?) {
    this.radioPressedListener = onRadioPressedListener
  }

  interface OnRadioPressedListener {
    fun onRadioPressed(path: String)
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: TextView by bindView(R.id.line_one)
    val context: ImageView by bindView(R.id.ui_item_context_indicator)
  }
}