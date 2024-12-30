package com.kelsos.mbrc.data

import com.kelsos.mbrc.data.db.RemoteDatabase
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.ConflictAction
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.annotation.Unique
import com.raizlabs.android.dbflow.annotation.UniqueGroup

@Table(
  name = "settings",
  database = RemoteDatabase::class,
  uniqueColumnGroups = [UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.IGNORE)],
)
data class ConnectionSettings(
  @Column(name = "address")
  @Unique(unique = false, uniqueGroups = [1])
  var address: String? = null,
  @Unique(unique = false, uniqueGroups = [1])
  @Column(name = "port")
  var port: Int = 0,
  @Column(name = "name")
  var name: String? = null,
  @PrimaryKey(autoincrement = true)
  @Column(name = "id")
  var id: Long = 0,
)
