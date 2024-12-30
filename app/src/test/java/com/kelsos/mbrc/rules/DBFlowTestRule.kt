package com.kelsos.mbrc.rules

import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment

class DBFlowTestRule : TestRule {
  override fun apply(
    base: Statement,
    description: Description,
  ): Statement =
    object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        FlowManager.init(FlowConfig.Builder(RuntimeEnvironment.application).build())
        try {
          base.evaluate()
        } finally {
          FlowManager.reset()
          FlowManager.destroy()
        }
      }
    }

  companion object {
    fun create(): DBFlowTestRule = DBFlowTestRule()
  }
}
