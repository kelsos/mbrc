package com.kelsos.mbrc.features.settings

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class ConnectionSettings(
  val address: String,
  val port: Int,
  val name: String,
  val isDefault: Boolean,
  val id: Long
) {
  companion object {
    fun default(): ConnectionSettings = ConnectionSettings(
      address = "",
      port = 3000,
      name = "",
      isDefault = false,
      id = 0
    )
  }
}

@Entity(
  tableName = "settings",
  indices = [Index(value = ["address", "port"], unique = true)]
)
data class ConnectionSettingsEntity(
  @ColumnInfo(name = "address")
  val address: String,
  @ColumnInfo(name = "port")
  val port: Int,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "is_default")
  val isDefault: Boolean? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0
)
