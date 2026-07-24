plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.kelsos.mbrc.core.queue"
  compileSdk = 37

  defaultConfig {
    minSdk = 23
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
  }
}

dependencies {
  implementation(project(":core:common"))

  testImplementation(libs.truth)
  testImplementation(libs.androidx.test.junit)
}
