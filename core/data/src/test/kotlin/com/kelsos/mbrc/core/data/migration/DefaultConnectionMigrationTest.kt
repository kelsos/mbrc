package com.kelsos.mbrc.core.data.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.settings.ConnectionDao
import com.kelsos.mbrc.core.data.settings.ConnectionSettingsEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Carries the user's default connection from SharedPreferences into the database, once, after
 * migration 3->4.
 *
 * The stakes are why `MIGRATION_1_4` goes out of its way to preserve row ids: this resolves the
 * default by id, so an id that shifted during the rebuild would silently point the user at a
 * different server.
 */
@RunWith(AndroidJUnit4::class)
class DefaultConnectionMigrationTest {

  private lateinit var database: Database
  private lateinit var connectionDao: ConnectionDao
  private lateinit var preferences: SharedPreferences
  private lateinit var migration: DefaultConnectionMigration

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room
      .inMemoryDatabaseBuilder(context, Database::class.java)
      .allowMainThreadQueries()
      .build()
    connectionDao = database.connectionDao()
    preferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
    preferences.edit().clear().commit()
    migration = DefaultConnectionMigration(preferences, connectionDao)
  }

  @After
  fun tearDown() {
    database.close()
  }

  private fun insertConnection(name: String, port: Int): Long = connectionDao.insert(
    ConnectionSettingsEntity(address = "192.168.1.$port", port = port, name = name)
  )

  private fun storedDefaultKey(id: Long) = preferences.edit().putLong(OLD_DEFAULT_KEY, id).commit()

  @Test
  fun `the connection named in preferences becomes the default`() {
    val other = insertConnection("other", 3000)
    val target = insertConnection("target", 3001)
    storedDefaultKey(target)

    val migrated = migration.migrate()

    assertThat(migrated).isTrue()
    assertThat(connectionDao.getDefault()?.id).isEqualTo(target)
    assertThat(connectionDao.getById(other)?.isDefault).isNull()
  }

  @Test
  fun `the old preference key is removed once it has been used`() {
    storedDefaultKey(insertConnection("target", 3001))

    migration.migrate()

    assertThat(preferences.contains(OLD_DEFAULT_KEY)).isFalse()
  }

  @Test
  fun `nothing happens when there is no key to migrate`() {
    insertConnection("only", 3000)

    val migrated = migration.migrate()

    assertThat(migrated).isFalse()
    assertThat(connectionDao.getDefault()).isNull()
  }

  @Test
  fun `an invalid stored id is discarded`() {
    insertConnection("only", 3000)
    storedDefaultKey(-1L)

    val migrated = migration.migrate()

    assertThat(migrated).isFalse()
    assertThat(connectionDao.getDefault()).isNull()
    assertThat(preferences.contains(OLD_DEFAULT_KEY)).isFalse()
  }

  @Test
  fun `an id with no matching connection leaves no default but still clears the key`() {
    insertConnection("only", 3000)
    storedDefaultKey(9_999L)

    val migrated = migration.migrate()

    assertThat(migrated).isFalse()
    assertThat(connectionDao.getDefault()).isNull()
    assertThat(preferences.contains(OLD_DEFAULT_KEY)).isFalse()
  }

  @Test
  fun `migrating twice is harmless because the key is gone`() {
    val target = insertConnection("target", 3001)
    storedDefaultKey(target)

    assertThat(migration.migrate()).isTrue()
    assertThat(migration.migrate()).isFalse()
    assertThat(connectionDao.getDefault()?.id).isEqualTo(target)
  }

  @Test
  fun `an existing default is replaced rather than added to`() {
    val previous = insertConnection("previous", 3000)
    val target = insertConnection("target", 3001)
    connectionDao.updateDefault(previous)
    storedDefaultKey(target)

    migration.migrate()

    assertThat(connectionDao.all().filter { it.isDefault == true }.map { it.id })
      .containsExactly(target)
  }

  private companion object {
    const val OLD_DEFAULT_KEY = "mbrc_default_settings"
  }
}
