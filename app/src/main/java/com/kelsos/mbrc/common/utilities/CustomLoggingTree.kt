package com.kelsos.mbrc.common.utilities

import timber.log.Timber

class CustomLoggingTree : Timber.DebugTree() {
  override fun createStackElementTag(element: StackTraceElement): String = with(element) {
    val thread = with(Thread.currentThread()) { name }
    val createStackElementTag = super.createStackElementTag(element)
    val className = createStackElementTag?.split("$")?.get(0)
    "($className.kt:$lineNumber)#$methodName {$thread}"
  }

  companion object {
    fun create(): CustomLoggingTree = CustomLoggingTree()
  }
}
