plugins {
  alias(libs.plugins.android.library)
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.kelsos.mbrc.core.data"
  compileSdk = 37

  defaultConfig {
    minSdk = 23
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

  testFixtures {
    enable = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  sourceSets {
    // Use debug assets for Robolectric tests - more reliable than test.assets
    getByName("debug").assets.srcDirs("$projectDir/schemas")
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  implementation(project(":core:common"))

  implementation(libs.androidx.core.ktx)
  implementation(libs.bundles.androidx.room)
  implementation(libs.androidx.paging.runtime.ktx)
  implementation(libs.koin.android)
  implementation(libs.squareup.moshi.lib)
  implementation(libs.squareup.okio)
  implementation(libs.timber)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.runtime)

  ksp(libs.androidx.room.compiler)
  ksp(libs.squareup.moshi.codegen)

  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(libs.kotlin.coroutines.test)

  // Test fixtures dependencies
  testFixturesImplementation(libs.koin.test)
  testFixturesImplementation(libs.androidx.test.core)
  testFixturesImplementation(libs.androidx.room.runtime)
}