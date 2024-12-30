import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt

plugins {
  alias(libs.plugins.versionsBenManes)
  alias(libs.plugins.versionCatalogUpdate)
  alias(libs.plugins.kover)
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlinAndroid) apply false
  alias(libs.plugins.kotlinter) apply false
  alias(libs.plugins.kapt) apply false
  alias(libs.plugins.kotlinParcelize) apply false
  alias(libs.plugins.detekt)
}

allprojects {
  apply {
    plugin("io.gitlab.arturbosch.detekt")
  }

  detekt {
    source.setFrom(objects.fileCollection().from(
      io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_JAVA,
      io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_JAVA,
      io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_KOTLIN,
      io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_KOTLIN,
    ))
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
  }

  tasks.withType<Detekt>().configureEach {
    reports {
      xml.required.set(true)
      html.required.set(true)
      sarif.required.set(true)
    }
  }

  buildscript {
    repositories {
      google()
      gradlePluginPortal()
      mavenCentral()
    }
  }

  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}


kover {}

// ReleaseType/DependencyUpdates are copied from:
// https://github.com/chrisbanes/tivi/blob/main/buildSrc/src/main/java/app/tivi/buildsrc/DependencyUpdates.kt
// Check header for license
enum class ReleaseType(private val level: Int) {
  SNAPSHOT(0),
  DEV(1),
  ALPHA(10),
  BETA(20),
  RC(60),
  RELEASE(100);

  fun isEqualOrMoreStableThan(other: ReleaseType): Boolean = level >= other.level

  fun isLessStableThan(other: ReleaseType): Boolean = level < other.level
}

object DependencyUpdates {
  private val stableKeywords = arrayOf("RELEASE", "FINAL", "GA")
  private val releaseRegex = "^[0-9,.v-]+(-r)?$".toRegex(RegexOption.IGNORE_CASE)
  private val rcRegex = releaseKeywordRegex("rc")
  private val betaRegex = releaseKeywordRegex("beta")
  private val alphaRegex = releaseKeywordRegex("alpha")
  private val devRegex = releaseKeywordRegex("dev")

  fun versionToRelease(version: String): ReleaseType {
    val stableKeyword = stableKeywords.any { version.uppercase().contains(it) }
    if (stableKeyword) return ReleaseType.RELEASE

    return when {
      releaseRegex.matches(version) -> ReleaseType.RELEASE
      rcRegex.matches(version) -> ReleaseType.RC
      betaRegex.matches(version) -> ReleaseType.BETA
      alphaRegex.matches(version) -> ReleaseType.ALPHA
      devRegex.matches(version) -> ReleaseType.DEV
      else -> ReleaseType.SNAPSHOT
    }
  }

  private fun releaseKeywordRegex(keyword: String): Regex {
    return "^[0-9,.v-]+(-$keyword[0-9]*)$".toRegex(RegexOption.IGNORE_CASE)
  }
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    val current = DependencyUpdates.versionToRelease(currentVersion)

    if (current == ReleaseType.SNAPSHOT) {
      return@rejectVersionIf true
    }

    val candidate = DependencyUpdates.versionToRelease(candidate.version)
    return@rejectVersionIf candidate.isLessStableThan(current)
  }
}

val dummyGoogleServices: Configuration by configurations.creating {
  isCanBeConsumed = true
  isCanBeResolved = false

  attributes {
    attribute(Attribute.of("google.services.json", String::class.java), "dummy-json")
  }
}

dependencies {
  dummyGoogleServices(files(rootProject.file("config/dummy-google-services.json")))
}

versionCatalogUpdate {
 keep {
   plugins.set(mutableListOf(
     libs.plugins.kotlinParcelize,
   ))
 }
}
