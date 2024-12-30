package com.kelsos.mbrc.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.constants.Const
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.ConnectionSettings
import com.kelsos.mbrc.data.SocketMessage
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.networking.RequestManager
import com.kelsos.mbrc.networking.RequestManagerImpl
import com.kelsos.mbrc.repository.ConnectionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import toothpick.config.Module
import toothpick.testing.ToothPickRule
import java.io.*
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ConnectionVerifierImplTest {
  private var server: ServerSocket? = null
  private val connectionRepository: ConnectionRepository = mockk()
  private val testDispatcher = StandardTestDispatcher()

  @Rule
  @JvmField
  val toothpickRule: ToothPickRule = ToothPickRule(this, "verifier")
    .setRootRegistryPackage("com.kelsos.mbrc")
  private val mapper = ObjectMapper()
  private val port: Int = 36000


  private lateinit var executor: ExecutorService

  @Before
  fun setUp() {
    toothpickRule.scope.installModules(TestModule())
    executor = Executors.newSingleThreadExecutor()
  }

  private fun startMockServer(
    prematureDisconnect: Boolean = false,
    responseContext: String = Protocol.VerifyConnection,
    json: Boolean = true
  ) {
    val random = Random()
    val mockSocket = Runnable {

      server = ServerSocket(port + random.nextInt(1000))

      while (true) {
        val connection = server?.accept()
        val input = InputStreamReader(connection!!.inputStream)
        val inputReader = BufferedReader(input)
        val line = inputReader.readLine()
        val value = mapper.readValue(line, SocketMessage::class.java)

        if (value.context != Protocol.VerifyConnection) {
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

  @After
  fun tearDown() {
    executor.shutdownNow()
  }

  @Test
  fun testSuccessfulVerification() = runTest(testDispatcher) {
    startMockServer()

    coEvery { connectionRepository.getDefault() } answers {
      val settings = ConnectionSettings()
      settings.address = server!!.inetAddress.hostAddress
      settings.port = server!!.localPort
      settings
    }

    val verifier = toothpickRule.getInstance(ConnectionVerifier::class.java)
    assertThat(verifier.verify()).isTrue()
  }


  @Test
  fun testPrematureDisconnectDuringVerification() = runTest(testDispatcher) {
    startMockServer(true)
    coEvery { connectionRepository.getDefault() } answers {
      val settings = ConnectionSettings()
      settings.address = server!!.inetAddress.hostAddress
      settings.port = server!!.localPort
      settings
    }

    val verifier = toothpickRule.getInstance(ConnectionVerifier::class.java)
    try {
      verifier.verify()
      error("Test should throw")
    } catch (e: Exception) {
      assertThat(e).isInstanceOf(RuntimeException::class.java)
    }
  }

  @Test
  fun testInvalidPluginResponseVerification() = runTest(testDispatcher) {
    startMockServer(false, Protocol.ClientNotAllowed)
    coEvery { connectionRepository.getDefault() } answers {
      val settings = ConnectionSettings()
      settings.address = server!!.inetAddress.hostAddress
      settings.port = server!!.localPort
      settings
    }

    val verifier = toothpickRule.getInstance(ConnectionVerifier::class.java)
    try {
      println(verifier.verify())
      error("Test should throw")
    } catch (e: Exception) {
      assertThat(e).isInstanceOf(ConnectionVerifierImpl.NoValidPluginConnection::class.java)
    }
  }

  @Test
  fun testVerificationNoConnection() = runTest(testDispatcher) {
    startMockServer(true)

    coEvery { connectionRepository.getDefault() } answers {
      null
    }

    val verifier = toothpickRule.getInstance(ConnectionVerifier::class.java)
    try {
      verifier.verify()
      error("Test should throw")
    } catch (e: Exception) {
      assertThat(e).isInstanceOf(RuntimeException::class.java)
    }
  }

  @Test
  fun testVerificationNoJsonPayload() = runTest(testDispatcher) {
    startMockServer(false, "payload", false)

    coEvery { connectionRepository.getDefault() } answers {
      null
    }

    val verifier = toothpickRule.getInstance(ConnectionVerifier::class.java)
    try {
      verifier.verify()
      error("Test should throw")
    } catch (e: Exception) {
      assertThat(e).isInstanceOf(RuntimeException::class.java)
    }
  }

  inner class TestModule : Module() {
    init {
      bind(ObjectMapper::class.java).toInstance(mapper)
      bind(RequestManager::class.java).to(RequestManagerImpl::class.java)
      bind(ConnectionRepository::class.java).toInstance(connectionRepository)
      bind(ConnectionVerifier::class.java).to(ConnectionVerifierImpl::class.java)
      bind(AppDispatchers::class.java).toInstance(
        AppDispatchers(
          testDispatcher,
          testDispatcher,
          testDispatcher
        )
      )
    }
  }
}
