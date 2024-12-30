import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.kover)
  alias(libs.plugins.detekt)
}

android {
  namespace = "com.kelsos.mbrc.changelog"

  defaultConfig {
    minSdk = 23
    compileSdk = 35

    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
    javaParameters.set(true)
  }
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.google.material)
  implementation(libs.kotlin.stdlib)
}
