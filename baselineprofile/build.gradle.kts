plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.baselineprofile)
}

android {
  namespace = "com.kelsos.mbrc.baselineprofile"
  compileSdk = 37

  defaultConfig {
    minSdk = 28
    targetSdk = 36
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

  targetProjectPath = ":app"

  flavorDimensions += "base"
  productFlavors {
    create("play") {}
    create("github") {}
  }
}

baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(libs.androidx.test.junit)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.benchmark.macro.junit4)
}
