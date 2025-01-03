package com.kelsos.mbrc.features.settings

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

data class ConnectionSettings(
  val address: String,
  val port: Int,
  val name: String,
  val isDefault: Boolean,
  val id: Long,
) {
  companion object {
    fun default(): ConnectionSettings =
      ConnectionSettings(
        address = "",
        port = 3000,
        name = "",
        isDefault = false,
        id = 0,
      )
  }
}

@Entity(
  tableName = "settings",
  indices = [],
)
data class ConnectionSettingsEntity(
  @ColumnInfo(name = "address")
  val address: String? = null,
  @ColumnInfo(name = "port")
  val port: Int? = null,
  @ColumnInfo(name = "name")
  val name: String? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null,
)
