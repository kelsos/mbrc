plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlinCompose)
}

android {
  namespace = "com.kelsos.mbrc.feature.widgets"
  compileSdk = 37

  defaultConfig {
    minSdk = 23
  }

  buildFeatures {
    compose = true
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

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

dependencies {
  coreLibraryDesugaring(libs.com.android.tools.desugar)

  implementation(project(":core:common"))
  implementation(project(":core:ui"))
  implementation(project(":core:platform"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.bundles.androidx.compose)
  implementation(libs.google.material)

  implementation(libs.androidx.glance.appwidget)

  implementation(libs.koin.android)
  implementation(libs.timber)
  implementation(libs.coilKt.compose)
  implementation(libs.squareup.moshi.lib)

  testImplementation(testFixtures(project(":core:common")))
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.glance.testing)
  testImplementation(libs.androidx.glance.appwidget.testing)
}
