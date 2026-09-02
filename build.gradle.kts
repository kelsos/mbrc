
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

buildscript {
  dependencies {
    classpath(libs.kotlinx.serialization.json)
    // AGP 9 built-in Kotlin defaults to KGP 2.2.10; override it up to the project's Kotlin version.
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    classpath(
      "com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.gradlePlugin.ksp.get()}"
    )
  }
}

plugins {
  alias(libs.plugins.versionsBenManes)
  alias(libs.plugins.versionCatalogUpdate)
  alias(libs.plugins.kover)
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.test) apply false
  alias(libs.plugins.baselineprofile) apply false
  alias(libs.plugins.kotlinter) apply false
  alias(libs.plugins.kotlinParcelize) apply false
  alias(libs.plugins.detekt)
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}


allprojects {
  apply {
    plugin("io.gitlab.arturbosch.detekt")
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
  }
}

subprojects {
  // With AGP 9 built-in Kotlin there is no kotlin.android plugin to hook; every Android
  // module is a Kotlin module, so key kotlinter/kover off the Android plugins instead.
  listOf("com.android.application", "com.android.library", "com.android.test").forEach { pluginId ->
    pluginManager.withPlugin(pluginId) {
      apply(plugin = "org.jmailen.kotlinter")
      apply(plugin = "org.jetbrains.kotlinx.kover")
    }
  }

  pluginManager.withPlugin("com.android.library") {
    configure<com.android.build.api.dsl.LibraryExtension> {
      lint {
        lintConfig = rootProject.file("config/lint.xml")
        sarifReport = true
      }
    }
  }

  // Compose lint checks are only registered where Compose is on the classpath, so naming them in
  // config/lint.xml would make every non-Compose module report UnknownIssueId. Promote them here
  // instead, rather than silencing UnknownIssueId globally and losing the typo check on the ids
  // we do configure.
  pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
    val composeChecks = listOf(
      // State read and remember correctness: silently stale UI or a state object that is
      // recreated on every recomposition.
      "StateFlowValueCalledInComposition",
      "FlowOperatorInvokedInComposition",
      "UnrememberedMutableState",
      "ProduceStateDoesNotAssignValue",
      "MutableCollectionMutableState",
      "FrequentlyChangedStateReadInComposition",
      "CoroutineCreationDuringComposition",
      // LocalContext.current.resources survives a configuration change and goes stale; this is
      // the check that should have caught the plural read fixed in 9ff372ef.
      "LocalContextResourcesRead",
      "AutoboxingStateCreation",
      "UnusedBoxWithConstraintsScope",
      "OpaqueUnitKey",
      // API shape.
      "ModifierParameter",
      "ComposableNaming",
      "RememberReturnType",
      "ComposableLambdaParameterNaming",
      "ComposableLambdaParameterPosition"
    )
    pluginManager.withPlugin("com.android.library") {
      configure<com.android.build.api.dsl.LibraryExtension> {
        lint.error.addAll(composeChecks)
      }
    }
    pluginManager.withPlugin("com.android.application") {
      configure<com.android.build.api.dsl.ApplicationExtension> {
        lint.error.addAll(composeChecks)
      }
    }
  }

  // Run unit tests only on debug build type - release tests are redundant for JVM tests
  tasks.withType<Test>().configureEach {
    onlyIf {
      val isRelease = name.contains("Release", ignoreCase = true)
      val isPlay = name.contains("Play", ignoreCase = true)
      !isRelease && !isPlay
    }
  }
}


/**
 * Coverage is only meaningful over code somebody wrote. Room DAO/database implementations, Moshi
 * adapters, protobuf message classes and Compose's lambda singletons are all emitted by a tool, so
 * counting them measures the code generators rather than this project. Left in, they contributed
 * roughly 2,200 uncovered lines and pulled the reported figure several points below the real one.
 */
kover {
  reports {
    filters {
      excludes {
        androidGeneratedClasses()
        // The `$*` variants are not redundant: a glob on the class name stops at the `$`, so
        // `*_Impl` alone leaves Room's nested query classes (`TrackDao_Impl$searchByTitleDesc$1`)
        // counted as hand-written code. They were the entire remainder of the generated bucket.
        classes(
          "*JsonAdapter",
          "*JsonAdapter$*",
          "*_Impl",
          "*_Impl$*",
          "*ComposableSingletons*",
          "com.kelsos.mbrc.store.*"
        )
      }
    }
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

  // Kover coverage aggregation - includes all modules for unified coverage reports
  kover(project(":app"))
  kover(project(":core:common"))
  kover(project(":core:data"))
  kover(project(":core:networking"))
  kover(project(":core:platform"))
  kover(project(":core:queue"))
  kover(project(":core:ui"))
  kover(project(":feature:content"))
  kover(project(":feature:library"))
  kover(project(":feature:minicontrol"))
  kover(project(":feature:misc"))
  kover(project(":feature:playback"))
  kover(project(":feature:settings"))
  kover(project(":feature:widgets"))
}

tasks.register("staticAnalysisAll") {
  description = "Runs detekt (with type resolution), lint, and kotlinter on all modules"
  dependsOn(subprojects.flatMap { it.tasks.matching { t -> t.name == "lintKotlin" } })
  dependsOn(subprojects.flatMap { it.tasks.matching { t -> t.name == "lint" } })
}

// Attach one detekt task per subproject once every project is configured.
//
// detektDebug runs with type resolution, which is required for rules like
// RedundantSuspendModifier that inspect call sites; the bare `detekt` task skips
// type resolution and silently misses these. Unflavored library modules expose
// `detektDebug`, but the flavored `:app` module only has per-flavor tasks
// (`detektGithubDebug`, `detektPlayDebug`) and the baseline-profile test module
// has neither. Matching on the literal name `detektDebug` therefore silently
// skipped `:app` entirely (#333). Resolve one task per subproject with a
// fallback chain and fail loudly if a module exposes no detekt task at all, so a
// future naming change can never drop a module from analysis unnoticed.
gradle.projectsEvaluated {
  tasks.named("staticAnalysisAll").configure {
    for (sub in subprojects) {
      val detektTask = sub.tasks.findByName("detektDebug")
        ?: sub.tasks.findByName("detektGithubDebug")
        ?: sub.tasks.findByName("detekt")
        ?: throw GradleException(
          "staticAnalysisAll found no detekt task for ${sub.path}; refusing to " +
            "silently skip it. Expected detektDebug, detektGithubDebug or detekt.",
        )
      dependsOn(detektTask)
    }
  }
}

abstract class CollectSarifReportsTask : DefaultTask() {

  @get:Input
  abstract val subprojectBuildDirs: ListProperty<File>

  @get:OutputDirectory
  abstract val reportsDir: DirectoryProperty

  @TaskAction
  fun execute() {
    val outputDir = reportsDir.get().asFile
    outputDir.mkdirs()

    val buildDirs = subprojectBuildDirs.get()

    // Collect and merge detekt SARIF reports. staticAnalysisAll runs the debug,
    // type-resolution task per module: library modules emit debug.sarif, the
    // flavored :app emits githubDebug.sarif; fall back to detekt.sarif for any
    // module that ran the bare task (e.g. the baseline-profile module).
    val detektFiles = buildDirs.mapNotNull { buildDir ->
      val debugSarif = File(buildDir, "reports/detekt/debug.sarif")
      val githubDebugSarif = File(buildDir, "reports/detekt/githubDebug.sarif")
      val baseSarif = File(buildDir, "reports/detekt/detekt.sarif")
      when {
        debugSarif.exists() -> debugSarif
        githubDebugSarif.exists() -> githubDebugSarif
        baseSarif.exists() -> baseSarif
        else -> null
      }
    }
    if (detektFiles.isNotEmpty()) {
      mergeSarifFiles(detektFiles, File(outputDir, "detekt.sarif"))
      logger.lifecycle("Merged ${detektFiles.size} detekt reports into detekt.sarif")
    }

    // Collect and merge Android lint SARIF reports (by variant)
    val allLintFiles = mutableListOf<File>()
    listOf("debug", "release", "githubDebug", "githubRelease").forEach { variant ->
      val lintFiles = buildDirs.mapNotNull { buildDir ->
        val sarifFile = File(buildDir, "reports/lint-results-$variant.sarif")
        if (sarifFile.exists()) sarifFile else null
      }
      if (lintFiles.isNotEmpty()) {
        mergeSarifFiles(lintFiles, File(outputDir, "lint-$variant.sarif"))
        logger.lifecycle("Merged ${lintFiles.size} lint reports into lint-$variant.sarif")
        allLintFiles.addAll(lintFiles)
      }
    }

    // Create unified android-lint.sarif with all variants
    if (allLintFiles.isNotEmpty()) {
      mergeSarifFiles(allLintFiles, File(outputDir, "android-lint.sarif"))
      logger.lifecycle("Merged ${allLintFiles.size} total lint reports into android-lint.sarif")
    }

    val files = outputDir.listFiles()?.filter { it.extension == "sarif" } ?: emptyList()
    logger.lifecycle("Created ${files.size} merged SARIF reports in ${outputDir.absolutePath}")
    files.forEach { logger.lifecycle("  - ${it.name}") }
  }

  private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

  private fun mergeSarifFiles(inputFiles: List<File>, outputFile: File) {
    var schema: String? = null
    var version: String? = null
    var tool: JsonElement? = null
    val allResults = mutableListOf<JsonElement>()
    val allArtifacts = mutableListOf<JsonElement>()

    inputFiles.forEach { file ->
      val report = json.parseToJsonElement(file.readText()).jsonObject
      schema = schema ?: report["\$schema"]?.jsonPrimitive?.content
      version = version ?: report["version"]?.jsonPrimitive?.content

      val runs = report["runs"]?.jsonArray ?: return@forEach

      runs.forEach { runElement ->
        val run = runElement.jsonObject
        tool = tool ?: run["tool"]

        val currentOffset = allArtifacts.size
        val artifacts = run["artifacts"]?.jsonArray ?: JsonArray(emptyList())
        allArtifacts.addAll(artifacts)

        val results = run["results"]?.jsonArray ?: JsonArray(emptyList())
        val adjustedResults = if (currentOffset > 0 && artifacts.isNotEmpty()) {
          results.map { result -> adjustArtifactIndices(result.jsonObject, currentOffset) }
        } else {
          results.toList()
        }
        allResults.addAll(adjustedResults)
      }
    }

    val merged = buildJsonObject {
      schema?.let { put("\$schema", it) }
      version?.let { put("version", it) }
      putJsonArray("runs") {
        add(buildJsonObject {
          tool?.let { put("tool", it) }
          putJsonArray("results") { allResults.forEach { add(it) } }
          if (allArtifacts.isNotEmpty()) {
            putJsonArray("artifacts") { allArtifacts.forEach { add(it) } }
          }
        })
      }
    }

    outputFile.writeText(json.encodeToString(JsonObject.serializer(), merged))
  }

  private fun adjustArtifactIndices(result: JsonObject, offset: Int): JsonObject {
    val locations = result["locations"]?.jsonArray ?: return result
    val adjustedLocations = locations.map { locationElement ->
      val location = locationElement.jsonObject
      val physicalLocation = location["physicalLocation"]?.jsonObject ?: return@map locationElement
      val artifactLocation = physicalLocation["artifactLocation"]?.jsonObject ?: return@map locationElement
      val index = artifactLocation["index"]?.jsonPrimitive?.intOrNull ?: return@map locationElement

      // Rebuild the nested structure with adjusted index
      val newArtifactLocation = buildJsonObject {
        artifactLocation.forEach { (key, value) ->
          if (key == "index") put(key, index + offset) else put(key, value)
        }
      }
      val newPhysicalLocation = buildJsonObject {
        physicalLocation.forEach { (key, value) ->
          if (key == "artifactLocation") put(key, newArtifactLocation) else put(key, value)
        }
      }
      buildJsonObject {
        location.forEach { (key, value) ->
          if (key == "physicalLocation") put(key, newPhysicalLocation) else put(key, value)
        }
      }
    }

    return buildJsonObject {
      result.forEach { (key, value) ->
        if (key == "locations") putJsonArray("locations") { adjustedLocations.forEach { add(it) } }
        else put(key, value)
      }
    }
  }
}

tasks.register<CollectSarifReportsTask>("collectSarifReports") {
  description = "Merges all SARIF reports from all modules into single files per tool"
  mustRunAfter("staticAnalysisAll")
  subprojectBuildDirs.set(subprojects.map { it.layout.buildDirectory.asFile.get() })
  reportsDir.set(layout.buildDirectory.dir("reports/sarif"))
}

tasks.register("testAll") {
  description = "Runs unit tests on all modules"
  dependsOn(subprojects.flatMap { it.tasks.matching { t -> t.name == "test" } })
}

tasks.register("verifyLocalAll") {
  description = "Runs all local verification checks on all modules"
  dependsOn("staticAnalysisAll")
  dependsOn("testAll")
  // App-specific tasks
  dependsOn(":app:validateGithubDebugScreenshotTest")
}

tasks.register("verifyAll") {
  description = "Runs all verification checks including instrumentation tests"
  dependsOn("verifyLocalAll")
  dependsOn(subprojects.flatMap { it.tasks.matching { t -> t.name == "connectedDebugAndroidTest" } })
}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    // Don't upgrade from stable to non-stable versions
    (isNonStable(candidate.version) && !isNonStable(currentVersion)) ||
      // Keep Kotlin version on the version specified in libs.versions.toml
      (candidate.module == "kotlin-gradle-plugin" && candidate.version != libs.versions.kotlin.get()) ||
      // KSP versions are compound versions, starting with the kotlin version
      (candidate.group == "com.google.devtools.ksp" && !candidate.version.startsWith(libs.versions.kotlin.get()))
  }
}
