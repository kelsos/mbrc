package com.kelsos.mbrc.ui.connection_manager

import android.support.v4.content.ContextCompat
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.kelsos.mbrc.R
import com.kelsos.mbrc.networking.connections.ConnectionSettings
import java.util.*

class ConnectionAdapter : RecyclerView.Adapter<ConnectionAdapter.ConnectionViewHolder>() {
  private val data: MutableList<ConnectionSettings>
  private var selectionId: Long = 0
  private var changeListener: ConnectionChangeListener? = null

  init {
    data = ArrayList<ConnectionSettings>()
    setHasStableIds(true)
  }

  fun setSelectionId(selectionId: Long) {
    this.selectionId = selectionId
    notifyDataSetChanged()
  }

  fun setChangeListener(changeListener: ConnectionChangeListener) {
    this.changeListener = changeListener
  }

  override fun getItemId(position: Int): Long {
    return data[0].id
  }

  override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ConnectionViewHolder {
    val inflater = LayoutInflater.from(viewGroup.context)
    val view = inflater.inflate(R.layout.ui_list_connection_settings, viewGroup, false)
    val holder = ConnectionViewHolder(view)

    holder.overflow.setOnClickListener {
      val adapterPosition = holder.adapterPosition
      val settings = data[adapterPosition]
      showPopup(settings, it)
    }

    holder.itemView.setOnClickListener {
      val adapterPosition = holder.adapterPosition
      val settings = data[adapterPosition]
      changeListener?.onDefault(settings)
    }
    return holder
  }

  override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
    val settings = data[position]
    holder.computerName.text = settings.name
    holder.hostname.text = settings.address
    holder.portNum.text = settings.port.toString()

    if (settings.id == selectionId) {
      holder.defaultSettings.visibility = View.VISIBLE
      val grey = ContextCompat.getColor(holder.itemView.context, R.color.button_dark)
      holder.defaultSettings.setImageResource(R.drawable.ic_check_black_24dp)
      holder.defaultSettings.setColorFilter(grey)
    } else {
      holder.defaultSettings.visibility = View.INVISIBLE
    }
  }

  private fun showPopup(settings: ConnectionSettings, v: View) {
    val popupMenu = PopupMenu(v.context, v)
    popupMenu.menuInflater.inflate(R.menu.connection_popup, popupMenu.menu)
    popupMenu.setOnMenuItemClickListener {
      if (changeListener == null) {
        return@setOnMenuItemClickListener false
      }

      when (it.itemId) {
        R.id.connection_default -> changeListener?.onDefault(settings)
        R.id.connection_edit -> changeListener?.onEdit(settings)
        R.id.connection_delete -> changeListener?.onDelete(settings)
        else -> {
        }
      }
      true
    }
    popupMenu.show()
  }

  override fun getItemCount(): Int {
    return data.size
  }

  fun update(connectionModel: ConnectionModel) {
    this.data.clear()
    this.data.addAll(connectionModel.settings)
    selectionId = connectionModel.defaultId
    notifyDataSetChanged()
  }

  inner class ConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @BindView(R.id.cs_list_host) lateinit var hostname: TextView
    @BindView(R.id.cs_list_port) lateinit var portNum: TextView
    @BindView(R.id.cs_list_name) lateinit var computerName: TextView
    @BindView(R.id.cs_list_default) lateinit var defaultSettings: ImageView
    @BindView(R.id.cs_list_overflow) lateinit var overflow: View

    init {
      ButterKnife.bind(this, itemView)
    }
  }

  interface ConnectionChangeListener {
    fun onDelete(settings: ConnectionSettings)

    fun onEdit(settings: ConnectionSettings)

    fun onDefault(settings: ConnectionSettings)
  }
}
