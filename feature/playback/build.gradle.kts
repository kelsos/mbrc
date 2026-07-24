plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlinCompose)
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.kelsos.mbrc.feature.playback"
  compileSdk = 37

  defaultConfig {
    minSdk = 23
  }

  buildFeatures {
    compose = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
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
  coreLibraryDesugaring(libs.com.android.tools.desugar)

  implementation(project(":core:common"))
  implementation(project(":core:ui"))
  implementation(project(":core:data"))
  implementation(project(":core:networking"))
  implementation(project(":feature:minicontrol"))
  implementation(project(":feature:misc"))
  implementation(project(":feature:settings"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.bundles.androidx.compose)
  implementation(libs.google.material)
  implementation(libs.androidx.paging.compose)
  implementation(libs.androidx.paging.runtime.ktx)

  implementation(libs.koin.android)
  implementation(libs.koin.compose)

  implementation(libs.coilKt.compose)
  implementation(libs.timber)
  implementation(libs.squareup.moshi.lib)
  implementation(libs.androidx.palette)
  ksp(libs.squareup.moshi.codegen)

  testImplementation(testFixtures(project(":core:common")))
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.androidx.arch.core.testing)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.runner)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.androidx.test.truth)
  testImplementation(libs.androidx.paging.common.ktx)
  testImplementation(libs.androidx.paging.testing)
  testImplementation(libs.turbine)
  testImplementation(libs.truth)
  testImplementation(libs.koin.test)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
