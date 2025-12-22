plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.kotlinSerialization)
  application
}

application {
  mainClass.set("com.kelsos.mbrc.mock.MainKt")
}

dependencies {
  implementation(libs.kotlin.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
}

tasks.jar {
  manifest {
    attributes["Main-Class"] = "com.kelsos.mbrc.mock.MainKt"
  }
  from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
