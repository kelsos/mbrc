package com.kelsos.mbrc.data

import com.kelsos.mbrc.data.db.RemoteDatabase
import com.raizlabs.android.dbflow.annotation.*
import com.raizlabs.android.dbflow.kotlinextensions.modelAdapter
import com.raizlabs.android.dbflow.structure.Model


@Table(name = "settings",
    database = RemoteDatabase::class,
    uniqueColumnGroups = arrayOf(UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.IGNORE)))
data class ConnectionSettings(@Column(name = "address")
                              @Unique(unique = false, uniqueGroups = kotlin.intArrayOf(1))
                              var address: String? = null,
                              @Unique(unique = false, uniqueGroups = kotlin.intArrayOf(1))
                              @Column(name = "port")
                              var port: Int = 0,
                              @Column(name = "name")
                              var name: String? = null,
                              @PrimaryKey(autoincrement = true)
                              @Column(name = "id")
                              var id: Long = 0) : Model {
  /**
   * Loads from the database the most recent version of the model based on it's primary keys.
   */
  override fun load() = modelAdapter<ConnectionSettings>().load(this)

  override fun insert(): Long = modelAdapter<ConnectionSettings>().insert(this)

  override fun save() : Boolean = modelAdapter<ConnectionSettings>().save(this)

  override fun update() : Boolean = modelAdapter<ConnectionSettings>().update(this)

  override fun exists(): Boolean = modelAdapter<ConnectionSettings>().exists(this)

  override fun delete() : Boolean = modelAdapter<ConnectionSettings>().delete(this)

}
