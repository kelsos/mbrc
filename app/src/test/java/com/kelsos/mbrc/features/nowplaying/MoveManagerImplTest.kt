package com.kelsos.mbrc.features.nowplaying

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class MoveManagerImplTest : KoinTest {
  private val onMoveSubmit: (Int, Int) -> Unit = mockk(relaxed = true)

  private val testModule =
    module {
      singleOf(::MoveManagerImpl) {
        bind<MoveManager>()
      }
    }

  private val moveManager: MoveManager by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule)) }
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun moveWithValidPositions() {
    moveManager.move(from = 0, to = 5)
    moveManager.onMoveCommit(onMoveSubmit)

    moveManager.commit()

    verify { onMoveSubmit(0, 5) }
  }

  @Test(expected = IllegalArgumentException::class)
  fun moveWithNegativeFromPosition() {
    moveManager.move(from = -1, to = 5)
  }

  @Test(expected = IllegalArgumentException::class)
  fun moveWithNegativeToPosition() {
    moveManager.move(from = 0, to = -1)
  }

  @Test
  fun multipleMovesPreservesOriginalPosition() {
    moveManager.move(from = 0, to = 2)
    moveManager.move(from = 2, to = 5)
    moveManager.move(from = 5, to = 8)
    moveManager.onMoveCommit(onMoveSubmit)

    moveManager.commit()

    verify { onMoveSubmit(0, 8) }
  }

  @Test
  fun commitWithoutSettingCallback() {
    moveManager.move(from = 0, to = 5)

    moveManager.commit()
  }

  @Test
  fun commitWithoutMoves() {
    moveManager.onMoveCommit(onMoveSubmit)

    moveManager.commit()

    verify(exactly = 0) { onMoveSubmit(any(), any()) }
  }

  @Test
  fun commitWithSameFromAndToPositions() {
    moveManager.move(from = 5, to = 5)
    moveManager.onMoveCommit(onMoveSubmit)

    moveManager.commit()

    verify(exactly = 0) { onMoveSubmit(any(), any()) }
  }

  @Test
  fun commitResetsPositionsAfterSubmission() {
    moveManager.move(from = 0, to = 5)
    moveManager.onMoveCommit(onMoveSubmit)

    moveManager.commit()
    moveManager.commit()

    verify(exactly = 1) { onMoveSubmit(0, 5) }
  }

  @Test
  fun multipleCommitCycles() {
    moveManager.onMoveCommit(onMoveSubmit)

    moveManager.move(from = 0, to = 3)
    moveManager.commit()

    moveManager.move(from = 1, to = 4)
    moveManager.commit()

    verifyOrder {
      onMoveSubmit(0, 3)
      onMoveSubmit(1, 4)
    }
  }

  @Test
  fun moveToSamePositionRepeatedlyUpdatesWhenDifferent() {
    moveManager.move(from = 0, to = 3)
    moveManager.move(from = 3, to = 3)
    moveManager.move(from = 3, to = 6)
    moveManager.onMoveCommit(onMoveSubmit)

    moveManager.commit()

    verify { onMoveSubmit(0, 6) }
  }

  @Test
  fun onMoveCommitCanBeCalledMultipleTimes() {
    val secondCallback: (Int, Int) -> Unit = mockk(relaxed = true)

    moveManager.onMoveCommit(onMoveSubmit)
    moveManager.onMoveCommit(secondCallback)
    moveManager.move(from = 0, to = 5)

    moveManager.commit()

    verify(exactly = 0) { onMoveSubmit(any(), any()) }
    verify { secondCallback(0, 5) }
  }
}
