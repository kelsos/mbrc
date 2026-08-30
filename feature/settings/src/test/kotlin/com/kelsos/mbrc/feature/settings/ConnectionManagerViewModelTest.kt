package com.kelsos.mbrc.feature.settings

import androidx.paging.PagingData
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.data.ConnectionSettings
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.networking.ClientConnectionUseCase
import com.kelsos.mbrc.core.networking.discovery.DiscoveryStop
import com.kelsos.mbrc.feature.settings.domain.ConnectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConnectionManagerViewModelTest : KoinTest {

  private lateinit var viewModel: ConnectionManagerViewModel
  private lateinit var repository: ConnectionRepository
  private lateinit var clientConnectionUseCase: ClientConnectionUseCase

  private val testModule = module {
    single { repository }
  }

  @Before
  fun setUp() {
    repository = mockk(relaxed = true) {
      every { getAll() } returns flowOf(PagingData.empty())
    }
    clientConnectionUseCase = mockk(relaxed = true)

    startKoin { modules(listOf(testModule, testDispatcherModule)) }

    viewModel = ConnectionManagerViewModel(
      repository = repository,
      dispatchers = org.koin.java.KoinJavaComponent.get(
        com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers::class.java
      ),
      clientConnectionUseCase = clientConnectionUseCase
    )
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  // region Initial State Tests

  @Test
  fun `initial dialogState should be Hidden`() = runTest(testDispatcher) {
    viewModel.dialogState.test {
      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Hidden)
    }
  }

  @Test
  fun `initial scanningState should be Idle`() = runTest(testDispatcher) {
    viewModel.scanningState.test {
      assertThat(awaitItem()).isEqualTo(ScanningState.Idle)
    }
  }

  @Test
  fun `initial fabExpanded should be false`() = runTest(testDispatcher) {
    viewModel.fabExpanded.test {
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `initial formState should have defaults`() = runTest(testDispatcher) {
    viewModel.formState.test {
      val state = awaitItem()
      assertThat(state.name).isEmpty()
      assertThat(state.address).isEmpty()
      assertThat(state.port).isEqualTo("3000")
      assertThat(state.portError).isNull()
      assertThat(state.isValid).isFalse()
    }
  }

  // endregion

  // region Dialog Tests

  @Test
  fun `showAddDialog should set dialogState to Add and reset form`() = runTest(testDispatcher) {
    viewModel.dialogState.test {
      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Hidden)

      viewModel.showAddDialog()

      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Add)
    }

    viewModel.formState.test {
      val state = awaitItem()
      assertThat(state.name).isEmpty()
      assertThat(state.address).isEmpty()
      assertThat(state.port).isEqualTo("3000")
    }
  }

  @Test
  fun `showAddDialog should collapse FAB menu`() = runTest(testDispatcher) {
    // First expand the FAB
    viewModel.toggleFabMenu()

    viewModel.fabExpanded.test {
      assertThat(awaitItem()).isTrue()

      viewModel.showAddDialog()

      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `showEditDialog should set dialogState to Edit with connection`() = runTest(testDispatcher) {
    val connection = ConnectionSettings(
      address = "192.168.1.100",
      port = 3000,
      name = "Test Server",
      isDefault = true,
      id = 1
    )

    viewModel.dialogState.test {
      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Hidden)

      viewModel.showEditDialog(connection)

      val state = awaitItem()
      assertThat(state).isInstanceOf(ConnectionDialogState.Edit::class.java)
      assertThat((state as ConnectionDialogState.Edit).connection).isEqualTo(connection)
    }
  }

  @Test
  fun `showEditDialog should populate form with connection data`() = runTest(testDispatcher) {
    val connection = ConnectionSettings(
      address = "192.168.1.100",
      port = 4000,
      name = "Test Server",
      isDefault = true,
      id = 1
    )

    viewModel.showEditDialog(connection)

    viewModel.formState.test {
      val state = awaitItem()
      assertThat(state.name).isEqualTo("Test Server")
      assertThat(state.address).isEqualTo("192.168.1.100")
      assertThat(state.port).isEqualTo("4000")
    }
  }

  @Test
  fun `hideDialog should set dialogState to Hidden and reset form`() = runTest(testDispatcher) {
    viewModel.showAddDialog()
    viewModel.updateAddress("192.168.1.1")
    viewModel.updateName("Test")

    viewModel.dialogState.test {
      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Add)

      viewModel.hideDialog()

      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Hidden)
    }

    viewModel.formState.test {
      val state = awaitItem()
      assertThat(state.name).isEmpty()
      assertThat(state.address).isEmpty()
    }
  }

  // endregion

  // region FAB Tests

  @Test
  fun `toggleFabMenu should toggle fabExpanded state`() = runTest(testDispatcher) {
    viewModel.fabExpanded.test {
      assertThat(awaitItem()).isFalse()

      viewModel.toggleFabMenu()
      assertThat(awaitItem()).isTrue()

      viewModel.toggleFabMenu()
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `collapseFabMenu should set fabExpanded to false`() = runTest(testDispatcher) {
    viewModel.toggleFabMenu() // Expand first

    viewModel.fabExpanded.test {
      assertThat(awaitItem()).isTrue()

      viewModel.collapseFabMenu()

      assertThat(awaitItem()).isFalse()
    }
  }

  // endregion

  // region Scanning Tests

  @Test
  fun `startScanning should set scanningState to Scanning`() = runTest(testDispatcher) {
    coEvery { repository.discover() } returns DiscoveryStop.NotFound

    viewModel.scanningState.test {
      assertThat(awaitItem()).isEqualTo(ScanningState.Idle)

      viewModel.startScanning()

      assertThat(awaitItem()).isEqualTo(ScanningState.Scanning)

      // Wait for discover to complete
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(ScanningState.Idle)
    }
  }

  @Test
  fun `startScanning should collapse FAB menu`() = runTest(testDispatcher) {
    coEvery { repository.discover() } returns DiscoveryStop.NotFound
    viewModel.toggleFabMenu()

    viewModel.fabExpanded.test {
      assertThat(awaitItem()).isTrue()

      viewModel.startScanning()

      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `startScanning should emit discovery event on completion`() = runTest(testDispatcher) {
    val discoveredConnection = ConnectionSettings(
      address = "192.168.1.50",
      port = 3000,
      name = "MusicBee",
      isDefault = false,
      id = 0
    )
    coEvery { repository.discover() } returns DiscoveryStop.Complete(listOf(discoveredConnection))

    viewModel.discoveryEvents.test {
      viewModel.startScanning()
      advanceUntilIdle()

      val event = awaitItem()
      assertThat(event).isInstanceOf(DiscoveryStop.Complete::class.java)
      assertThat((event as DiscoveryStop.Complete).settings).containsExactly(discoveredConnection)
    }
  }

  @Test
  fun `startScanning should emit NotFound when no server found`() = runTest(testDispatcher) {
    coEvery { repository.discover() } returns DiscoveryStop.NotFound

    viewModel.discoveryEvents.test {
      viewModel.startScanning()
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(DiscoveryStop.NotFound)
    }
  }

  @Test
  fun `startScanning should emit NoWifi when wifi not available`() = runTest(testDispatcher) {
    coEvery { repository.discover() } returns DiscoveryStop.NoWifi

    viewModel.discoveryEvents.test {
      viewModel.startScanning()
      advanceUntilIdle()

      assertThat(awaitItem()).isEqualTo(DiscoveryStop.NoWifi)
    }
  }

  @Test
  fun `stopScanning should set scanningState to Idle`() = runTest(testDispatcher) {
    coEvery { repository.discover() } coAnswers {
      // Simulate long running operation
      kotlinx.coroutines.delay(10000)
      DiscoveryStop.NotFound
    }

    viewModel.startScanning()

    viewModel.scanningState.test {
      assertThat(awaitItem()).isEqualTo(ScanningState.Scanning)

      viewModel.stopScanning()

      assertThat(awaitItem()).isEqualTo(ScanningState.Idle)
    }
  }

  // endregion

  // region Form Tests

  @Test
  fun `updateName should update form name field`() = runTest(testDispatcher) {
    viewModel.formState.test {
      assertThat(awaitItem().name).isEmpty()

      viewModel.updateName("My Server")

      assertThat(awaitItem().name).isEqualTo("My Server")
    }
  }

  @Test
  fun `updateAddress should update form address field`() = runTest(testDispatcher) {
    viewModel.formState.test {
      assertThat(awaitItem().address).isEmpty()

      viewModel.updateAddress("192.168.1.100")

      assertThat(awaitItem().address).isEqualTo("192.168.1.100")
    }
  }

  @Test
  fun `updatePort with valid port should update form without error`() = runTest(testDispatcher) {
    viewModel.formState.test {
      awaitItem() // initial

      viewModel.updatePort("5000", "Invalid port")

      val state = awaitItem()
      assertThat(state.port).isEqualTo("5000")
      assertThat(state.portError).isNull()
    }
  }

  @Test
  fun `updatePort with invalid port should set error`() = runTest(testDispatcher) {
    viewModel.formState.test {
      awaitItem() // initial

      viewModel.updatePort("99999", "Invalid port")

      val state = awaitItem()
      assertThat(state.port).isEqualTo("99999")
      assertThat(state.portError).isEqualTo("Invalid port")
    }
  }

  @Test
  fun `updatePort with zero should set error`() = runTest(testDispatcher) {
    viewModel.formState.test {
      awaitItem() // initial

      viewModel.updatePort("0", "Invalid port")

      val state = awaitItem()
      assertThat(state.port).isEqualTo("0")
      assertThat(state.portError).isEqualTo("Invalid port")
    }
  }

  @Test
  fun `updatePort with negative should set error`() = runTest(testDispatcher) {
    viewModel.formState.test {
      awaitItem() // initial

      viewModel.updatePort("-1", "Invalid port")

      val state = awaitItem()
      assertThat(state.portError).isEqualTo("Invalid port")
    }
  }

  @Test
  fun `updatePort with non-numeric should set error`() = runTest(testDispatcher) {
    viewModel.formState.test {
      awaitItem() // initial

      viewModel.updatePort("abc", "Invalid port")

      val state = awaitItem()
      assertThat(state.portError).isEqualTo("Invalid port")
    }
  }

  @Test
  fun `form isValid should be true when address is set and port is valid`() =
    runTest(testDispatcher) {
      viewModel.formState.test {
        assertThat(awaitItem().isValid).isFalse()

        viewModel.updateAddress("192.168.1.1")
        assertThat(awaitItem().isValid).isTrue()
      }
    }

  @Test
  fun `form isValid should be false when port has error`() = runTest(testDispatcher) {
    viewModel.updateAddress("192.168.1.1")
    viewModel.updatePort("99999", "Invalid port")

    viewModel.formState.test {
      val state = awaitItem()
      assertThat(state.isValid).isFalse()
    }
  }

  // endregion

  // region Connection Actions Tests

  @Test
  fun `saveConnection should call repository save for new connection`() = runTest(testDispatcher) {
    viewModel.showAddDialog()
    viewModel.updateAddress("192.168.1.100")
    viewModel.updateName("Test Server")
    viewModel.updatePort("3000", "Invalid")

    viewModel.saveConnection()
    advanceUntilIdle()

    coVerify {
      repository.save(
        match {
          it.address == "192.168.1.100" &&
            it.name == "Test Server" &&
            it.port == 3000
        }
      )
    }
  }

  @Test
  fun `saveConnection should call repository save for edited connection`() =
    runTest(testDispatcher) {
      val existingConnection = ConnectionSettings(
        address = "192.168.1.50",
        port = 3000,
        name = "Old Name",
        isDefault = true,
        id = 5
      )

      viewModel.showEditDialog(existingConnection)
      viewModel.updateName("New Name")
      viewModel.updateAddress("192.168.1.100")

      viewModel.saveConnection()
      advanceUntilIdle()

      coVerify {
        repository.save(
          match {
            it.id == 5L &&
              it.address == "192.168.1.100" &&
              it.name == "New Name" &&
              it.isDefault
          }
        )
      }
    }

  @Test
  fun `saveConnection should hide dialog after saving`() = runTest(testDispatcher) {
    viewModel.showAddDialog()
    viewModel.updateAddress("192.168.1.100")

    viewModel.dialogState.test {
      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Add)

      viewModel.saveConnection()

      assertThat(awaitItem()).isEqualTo(ConnectionDialogState.Hidden)
    }
  }

  @Test
  fun `saveConnection should not save when form is invalid`() = runTest(testDispatcher) {
    viewModel.showAddDialog()
    // Don't set address - form is invalid

    viewModel.saveConnection()
    advanceUntilIdle()

    coVerify(exactly = 0) { repository.save(any()) }
  }

  @Test
  fun `saveConnection should not save when port is invalid`() = runTest(testDispatcher) {
    viewModel.showAddDialog()
    viewModel.updateAddress("192.168.1.100")
    viewModel.updatePort("99999", "Invalid")

    viewModel.saveConnection()
    advanceUntilIdle()

    coVerify(exactly = 0) { repository.save(any()) }
  }

  @Test
  fun `deleteConnection should call repository delete`() = runTest(testDispatcher) {
    val connection = ConnectionSettings(
      address = "192.168.1.100",
      port = 3000,
      name = "Test",
      isDefault = false,
      id = 1
    )

    viewModel.deleteConnection(connection)
    advanceUntilIdle()

    coVerify { repository.delete(connection) }
  }

  @Test
  fun `setDefaultConnection should call repository setDefault and reconnect`() =
    runTest(testDispatcher) {
      val connection = ConnectionSettings(
        address = "192.168.1.100",
        port = 3000,
        name = "Test",
        isDefault = false,
        id = 1
      )

      viewModel.setDefaultConnection(connection)
      advanceUntilIdle()

      coVerify { repository.setDefault(connection) }
      coVerify { clientConnectionUseCase.connect(reset = true) }
    }

  // endregion

  // region ConnectionFormState Tests

  @Test
  fun `ConnectionFormState portNumber should parse valid port`() {
    val state = ConnectionFormState(port = "3000")
    assertThat(state.portNumber).isEqualTo(3000)
  }

  @Test
  fun `ConnectionFormState portNumber should return 0 for invalid port`() {
    val state = ConnectionFormState(port = "abc")
    assertThat(state.portNumber).isEqualTo(0)
  }

  @Test
  fun `ConnectionFormState isValid should be false when address is empty`() {
    val state = ConnectionFormState(address = "", port = "3000")
    assertThat(state.isValid).isFalse()
  }

  @Test
  fun `ConnectionFormState isValid should be false when portError is set`() {
    val state = ConnectionFormState(address = "192.168.1.1", port = "3000", portError = "Error")
    assertThat(state.isValid).isFalse()
  }

  @Test
  fun `ConnectionFormState isValid should be true when address is set and no port error`() {
    val state = ConnectionFormState(address = "192.168.1.1", port = "3000", portError = null)
    assertThat(state.isValid).isTrue()
  }

  // endregion
}
