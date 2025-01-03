package com.kelsos.mbrc.features.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kelsos.mbrc.R

interface ConnectionChangeListener {
  fun onDelete(settings: ConnectionSettings)

  fun onEdit(settings: ConnectionSettings)

  fun onDefault(settings: ConnectionSettings)
}

class ConnectionAdapter : PagingDataAdapter<ConnectionSettings, ConnectionAdapter.ConnectionViewHolder>(DIFF_CALLBACK) {
  private var changeListener: ConnectionChangeListener? = null

  fun setChangeListener(changeListener: ConnectionChangeListener) {
    this.changeListener = changeListener
  }

  override fun onCreateViewHolder(
    viewGroup: ViewGroup,
    position: Int,
  ): ConnectionViewHolder {
    val inflater = LayoutInflater.from(viewGroup.context)
    val view = inflater.inflate(R.layout.ui_list_connection_settings, viewGroup, false)
    val holder = ConnectionViewHolder(view)

    holder.overflow.setOnClickListener {
      val adapterPosition = holder.bindingAdapterPosition
      getItem(adapterPosition)?.let { settings ->
        showPopup(settings, it)
      }
    }

    holder.itemView.setOnClickListener {
      val adapterPosition = holder.bindingAdapterPosition
      getItem(adapterPosition)?.let { settings ->
        changeListener?.onDefault(settings)
      }
    }
    return holder
  }

  override fun onBindViewHolder(
    holder: ConnectionViewHolder,
    position: Int,
  ) {
    getItem(position)?.let { settings ->
      holder.computerName.text = settings.name
      holder.hostname.text = settings.address
      holder.portNum.text = holder.itemView.context.getString(R.string.common_number, settings.port)
      holder.defaultSettings.isVisible = settings.isDefault

      if (settings.isDefault) {
        val grey = ContextCompat.getColor(holder.itemView.context, R.color.button_dark)
        holder.defaultSettings.setImageResource(R.drawable.ic_check_black_24dp)
        holder.defaultSettings.setColorFilter(grey)
      }
    }
  }

  private fun showPopup(
    settings: ConnectionSettings,
    v: View,
  ) {
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
        else -> error("Unknown menu item id: ${it.itemId}")
      }
      true
    }
    popupMenu.show()
  }

  inner class ConnectionViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    val hostname: TextView = itemView.findViewById(R.id.cs_list_host)
    val portNum: TextView = itemView.findViewById(R.id.cs_list_port)
    val computerName: TextView = itemView.findViewById(R.id.cs_list_name)
    val defaultSettings: ImageView = itemView.findViewById(R.id.cs_list_default)
    val overflow: View = itemView.findViewById(R.id.cs_list_overflow)
  }

  companion object {
    private val DIFF_CALLBACK =
      object : DiffUtil.ItemCallback<ConnectionSettings>() {
        override fun areItemsTheSame(
          oldItem: ConnectionSettings,
          newItem: ConnectionSettings,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
          oldItem: ConnectionSettings,
          newItem: ConnectionSettings,
        ): Boolean = oldItem == newItem
      }
  }
}
