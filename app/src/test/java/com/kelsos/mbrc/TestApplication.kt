package com.kelsos.mbrc

import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

class TestApplication :
  App(),
  TestLifecycleApplication {
  override fun testMode(): Boolean = true

  override fun beforeTest(method: Method?) {
  }

  override fun prepareTest(test: Any?) {
  }

  override fun afterTest(method: Method?) {
  }
}
