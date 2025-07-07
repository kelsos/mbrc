package com.kelsos.mbrc.networking.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.utils.testDispatcherModule
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class ConnectionTest : KoinTest {
  private val testModule = module {
    single { Moshi.Builder().build() }
  }

  private val moshi: Moshi by inject()
  private val dispatchers by inject<AppCoroutineDispatchers>()

  private lateinit var server: ServerSocket
  private lateinit var executor: ExecutorService

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule, testDispatcherModule)) }
    executor = Executors.newCachedThreadPool()
  }

  @After
  fun tearDown() {
    if (::server.isInitialized && !server.isClosed) {
      server.close()
    }
    executor.shutdownNow()
    stopKoin()
  }

  private fun createTestServer(port: Int = 0): ServerSocket {
    server = ServerSocket(port)
    server.soTimeout = 2000
    return server
  }

  private fun createConnection(
    socket: Socket,
    config: ConnectionConfig = ConnectionConfig()
  ): Connection = Connection(socket, moshi, dispatchers, config)

  @Test
  fun connectionShouldDetectHealthySocketState() {
    runTest {
      // Given
      val server = createTestServer()
      val clientSocket = Socket()
      clientSocket.connect(server.localSocketAddress)

      executor.execute {
        try {
          server.accept()
        } catch (e: Exception) {
          // Expected when test completes - server socket closed
          if (!server.isClosed) {
            throw e
          }
        }
      }

      val connection = createConnection(clientSocket)

      // When & Then
      assertThat(connection.isConnected).isTrue()
      assertThat(connection.isSocketHealthy()).isTrue()

      connection.cleanup()
    }
  }

  @Test
  fun connectionShouldDetectUnhealthySocketStateAfterCleanup() {
    runTest {
      // Given
      val server = createTestServer()
      val clientSocket = Socket()
      clientSocket.connect(server.localSocketAddress)

      executor.execute {
        try {
          server.accept()
        } catch (e: Exception) {
          // Expected when test completes - server socket closed
          if (!server.isClosed) {
            throw e
          }
        }
      }

      val connection = createConnection(clientSocket)

      // When
      connection.cleanup()

      // Then
      assertThat(connection.isConnected).isFalse()
    }
  }

  @Test
  fun socketCreationShouldApplyCorrectSettings() {
    runTest {
      // Given
      val server = createTestServer()

      // When
      val socket = Connection.connect(server.localSocketAddress)

      // Then
      assertThat(socket.tcpNoDelay).isTrue()
      assertThat(socket.keepAlive).isTrue()
      assertThat(socket.soTimeout).isEqualTo(30_000)

      socket.close()
    }
  }

  @Test
  fun readTimeoutShouldWorkCorrectly() {
    runTest {
      // Given
      val server = createTestServer()
      val clientSocket = Socket()
      clientSocket.connect(server.localSocketAddress)

      // Server that accepts but never sends data
      executor.execute {
        try {
          val serverSocket = server.accept()
          // Don't send anything, just wait
          Thread.sleep(2000)
          serverSocket.close()
        } catch (e: Exception) {
          // Expected when test completes - server socket closed or interrupted
          if (!server.isClosed && !Thread.currentThread().isInterrupted) {
            throw e
          }
        }
      }

      val shortTimeoutConfig = ConnectionConfig(readTimeoutMs = 500L)
      val connection = createConnection(clientSocket, shortTimeoutConfig)

      // When
      connection.listen() // Should timeout internally and cleanup connection

      // Then - After timeout, connection should be disconnected
      assertThat(connection.isConnected).isFalse()
    }
  }

  @Test
  fun cleanupShouldBeIdempotent() {
    runTest {
      // Given
      val server = createTestServer()
      val clientSocket = Socket()
      clientSocket.connect(server.localSocketAddress)

      executor.execute {
        try {
          server.accept()
        } catch (e: Exception) {
          // Expected when test completes - server socket closed
          if (!server.isClosed) {
            throw e
          }
        }
      }

      val connection = createConnection(clientSocket)

      // When
      connection.cleanup()
      connection.cleanup() // Should not throw or cause issues
      connection.cleanup()

      // Then
      assertThat(connection.isConnected).isFalse()
    }
  }
}
