package com.kelsos.mbrc

import android.app.Application
import org.robolectric.TestLifecycleApplication
import timber.log.Timber
import java.lang.reflect.Method

class TestApplication :
  Application(),
  TestLifecycleApplication {
  override fun beforeTest(method: Method?) {
    Timber.plant(
      object : Timber.DebugTree() {
        override fun createStackElementTag(element: StackTraceElement): String =
          with(element) {
            val thread = with(Thread.currentThread()) { name }
            val createStackElementTag = super.createStackElementTag(element)
            val className = createStackElementTag?.split("$")?.get(0)
            "($className.kt:$lineNumber)#$methodName {$thread}"
          }

        override fun log(
          priority: Int,
          tag: String?,
          message: String,
          t: Throwable?,
        ) {
          println("$tag: $message")
        }
      },
    )
    setTheme(R.style.Theme_App)
  }

  override fun prepareTest(test: Any?) = Unit

  override fun afterTest(method: Method?) {
    Timber.uprootAll()
  }
}
