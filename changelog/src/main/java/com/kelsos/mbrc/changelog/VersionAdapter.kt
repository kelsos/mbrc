package com.kelsos.mbrc.changelog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.recyclerview.widget.RecyclerView

class VersionAdapter(
  private val changeLog: List<ChangeLogEntry>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  class VersionViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    private val version: TextView = itemView.findViewById(R.id.changelog_version__version)
    private val release: TextView = itemView.findViewById(R.id.changelog_version__release)

    fun bind(version: ChangeLogEntry.Version) {
      this.version.text = version.version
      this.release.text = version.release
    }

    companion object {
      fun from(parent: ViewGroup): VersionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.changelog_dialog__header, parent, false)
        return VersionViewHolder(itemView)
      }
    }
  }

  class EntryViewHolder(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView) {
    private val text: TextView = itemView.findViewById(R.id.changelog_entry__text)
    private val type: TextView = itemView.findViewById(R.id.changelog_entry__type)

    fun bind(entry: ChangeLogEntry.Entry) {
      val typeResId =
        when (entry.type) {
          is EntryType.BUG -> R.string.entry__bug
          is EntryType.FEATURE -> R.string.entry__feature
          is EntryType.REMOVED -> R.string.entry__removed
        }
      type.setText(typeResId)
      text.text = HtmlCompat.fromHtml(entry.text, FROM_HTML_MODE_LEGACY)
    }

    companion object {
      fun from(parent: ViewGroup): EntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.changelog_dialog__entry, parent, false)
        return EntryViewHolder(itemView)
      }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): RecyclerView.ViewHolder =
    when (viewType) {
      ITEM_VIEW_TYPE_HEADER -> VersionViewHolder.from(parent)
      ITEM_VIEW_TYPE_ITEM -> EntryViewHolder.from(parent)
      else -> throw ClassCastException("Unknown viewType $viewType")
    }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    val version = changeLog[position]
    when (holder) {
      is VersionViewHolder -> holder.bind(version as ChangeLogEntry.Version)
      is EntryViewHolder -> holder.bind(version as ChangeLogEntry.Entry)
    }
  }

  override fun getItemViewType(position: Int): Int =
    when (changeLog[position]) {
      is ChangeLogEntry.Version -> ITEM_VIEW_TYPE_HEADER
      is ChangeLogEntry.Entry -> ITEM_VIEW_TYPE_ITEM
    }

  override fun getItemCount(): Int = changeLog.size

  companion object {
    private const val ITEM_VIEW_TYPE_HEADER = 0
    private const val ITEM_VIEW_TYPE_ITEM = 1
  }
}
