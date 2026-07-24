plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlinCompose)
}

android {
  namespace = "com.kelsos.mbrc.core.ui"
  compileSdk = 37

  defaultConfig {
    minSdk = 23
  }

  buildFeatures {
    compose = true
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    unitTests.isIncludeAndroidResources = true
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

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.bundles.androidx.compose)
  implementation(libs.google.material)
  implementation(libs.androidx.paging.compose)

  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.runner)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
