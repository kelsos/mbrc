package com.kelsos.mbrc.networking.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.constants.Const
import com.kelsos.mbrc.features.settings.ConnectionRepository
import com.kelsos.mbrc.features.settings.ConnectionSettings
import com.kelsos.mbrc.networking.RequestManager
import com.kelsos.mbrc.networking.RequestManagerImpl
import com.kelsos.mbrc.networking.protocol.Protocol
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.Random
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ConnectivityVerifierImplTest : KoinTest {
  private var server: ServerSocket? = null
  private val connectionRepository: ConnectionRepository by inject()
  private val testDispatcher = StandardTestDispatcher()
  private val verifier: ConnectivityVerifier by inject()

  private val mapper = ObjectMapper()
  private val port: Int = 36000

  private lateinit var executor: ExecutorService

  private val testModule =
    module {
      single { mapper }
      single { mockk<ConnectionRepository>() }
      singleOf(::RequestManagerImpl) { bind<RequestManager>() }
      singleOf(::ConnectivityVerifierImpl) { bind<ConnectivityVerifier>() }
      single {
        AppCoroutineDispatchers(
          testDispatcher,
          testDispatcher,
          testDispatcher,
          testDispatcher,
        )
      }
    }

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule))
    }
    executor = Executors.newSingleThreadExecutor()
  }

  @After
  fun tearDown() {
    executor.shutdownNow()
    stopKoin()
  }

  private fun startMockServer(
    prematureDisconnect: Boolean = false,
    responseContext: String = Protocol.VERIFY_CONNECTION,
    json: Boolean = true,
  ) {
    val random = Random()
    val mockSocket =
      Runnable {
        server = ServerSocket(port + random.nextInt(1000))

        while (true) {
          val connection = server?.accept()
          val input = InputStreamReader(connection!!.inputStream)
          val inputReader = BufferedReader(input)
          val line = inputReader.readLine()
          val value = mapper.readValue(line, SocketMessage::class.java)

          if (value.context != Protocol.VERIFY_CONNECTION) {
            connection.close()
            server?.close()
            return@Runnable
          }

          if (prematureDisconnect) {
            connection.close()
            server?.close()
            return@Runnable
          }

          val out = OutputStreamWriter(connection.outputStream, Const.UTF_8)
          val output = PrintWriter(BufferedWriter(out), true)
          if (json) {
            val response = SocketMessage()
            response.context = responseContext
            output.write(mapper.writeValueAsString(response) + "\n\r")
          } else {
            output.write(responseContext + "\n\r")
          }
          output.flush()
          input.close()
          inputReader.close()
          out.close()
          output.close()
          connection.close()
          server?.close()
          return@Runnable
        }
      }

    executor.execute(mockSocket)
  }

  @Test
  fun testSuccessfulVerification() =
    runTest(testDispatcher) {
      startMockServer()

      coEvery { connectionRepository.getDefault() } answers {
        val settings = ConnectionSettings()
        settings.address = server!!.inetAddress.hostAddress
        settings.port = server!!.localPort
        settings
      }

      assertThat(verifier.verify()).isTrue()
    }

  @Test
  fun testPrematureDisconnectDuringVerification() =
    runTest(testDispatcher) {
      startMockServer(true)
      coEvery { connectionRepository.getDefault() } answers {
        val settings = ConnectionSettings()
        settings.address = server!!.inetAddress.hostAddress
        settings.port = server!!.localPort
        settings
      }

      try {
        verifier.verify()
        error("Test should throw")
      } catch (e: Exception) {
        assertThat(e).isInstanceOf(RuntimeException::class.java)
      }
    }

  @Test
  fun testInvalidPluginResponseVerification() =
    runTest(testDispatcher) {
      startMockServer(false, Protocol.CLIENT_NOT_ALLOWED)
      coEvery { connectionRepository.getDefault() } answers {
        val settings = ConnectionSettings()
        settings.address = server!!.inetAddress.hostAddress
        settings.port = server!!.localPort
        settings
      }

      try {
        println(verifier.verify())
        error("Test should throw")
      } catch (e: Exception) {
        assertThat(e).isInstanceOf(ConnectivityVerifierImpl.NoValidPluginConnection::class.java)
      }
    }

  @Test
  fun testVerificationNoConnection() =
    runTest(testDispatcher) {
      startMockServer(true)

      coEvery { connectionRepository.getDefault() } answers {
        null
      }

      try {
        verifier.verify()
        error("Test should throw")
      } catch (e: Exception) {
        assertThat(e).isInstanceOf(RuntimeException::class.java)
      }
    }

  @Test
  fun testVerificationNoJsonPayload() =
    runTest(testDispatcher) {
      startMockServer(false, "payload", false)

      coEvery { connectionRepository.getDefault() } answers {
        null
      }

      try {
        verifier.verify()
        error("Test should throw")
      } catch (e: Exception) {
        assertThat(e).isInstanceOf(RuntimeException::class.java)
      }
    }
}
