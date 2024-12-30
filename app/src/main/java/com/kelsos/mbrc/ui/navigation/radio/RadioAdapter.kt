package com.kelsos.mbrc.ui.navigation.radio


import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R
import com.kelsos.mbrc.data.RadioStation
import com.raizlabs.android.dbflow.list.FlowCursorList
import javax.inject.Inject

class RadioAdapter
@Inject constructor(context: Activity) : RecyclerView.Adapter<RadioAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data: FlowCursorList<RadioStation>? = null
  private var radioPressedListener: OnRadioPressedListener? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = inflater.inflate(R.layout.listitem_single, parent, false)
    val viewHolder = ViewHolder(view)

    viewHolder.itemView.setOnClickListener {
      val path = data?.getItem(viewHolder.bindingAdapterPosition.toLong())?.url
      path?.let {
        radioPressedListener?.onRadioPressed(it)
      }

    }
    return viewHolder
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val radio = data?.getItem(holder.bindingAdapterPosition.toLong())
    radio?.let {
      holder.name.text = radio.name
    }
    holder.context.visibility = View.GONE

  }

  override fun getItemCount(): Int {
    return data?.count() ?: 0
  }

  fun update(cursor: FlowCursorList<RadioStation>) {
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
    val name: TextView = itemView.findViewById(R.id.line_one)
    val context: LinearLayout = itemView.findViewById(R.id.ui_item_context_indicator)
  }
}
