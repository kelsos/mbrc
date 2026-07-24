plugins {
  alias(libs.plugins.android.library)
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.kelsos.mbrc.core.networking"
  compileSdk = 37

  defaultConfig {
    minSdk = 23
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
      freeCompilerArgs.set(listOf(
        "-Xannotation-default-target=param-property"
      ))
      optIn.set(listOf(
        "kotlinx.coroutines.ExperimentalCoroutinesApi"
      ))
    }
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    unitTests.isIncludeAndroidResources = true
  }
}

dependencies {
  coreLibraryDesugaring(libs.com.android.tools.desugar)

  api(project(":core:common"))

  implementation(libs.bundles.coroutines)
  implementation(libs.koin.android)
  implementation(libs.squareup.moshi.lib)
  implementation(libs.squareup.okio)
  implementation(libs.timber)

  ksp(libs.squareup.moshi.codegen)

  // Test dependencies
  testImplementation(testFixtures(project(":core:common")))
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.runner)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.koin.test)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)
  testImplementation(libs.squareup.moshi.lib)
}
