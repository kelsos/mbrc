pluginManagement {
  repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
  }
}

rootProject.name = "mbrc"

include(":changelog")
include(":app")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
