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
include(":mock-server")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
