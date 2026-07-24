import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.gitlab.arturbosch.detekt.Detekt
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlinParcelize)
  alias(libs.plugins.kotlinCompose)
  id("com.google.devtools.ksp")
  alias(libs.plugins.protobuf)
  alias(libs.plugins.googleServices) apply false
  alias(libs.plugins.crashlytics) apply false
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.kover)
  alias(libs.plugins.detekt)
  alias(libs.plugins.screenshot)
  alias(libs.plugins.aboutlibraries)
  alias(libs.plugins.baselineprofile)
}

object KeyLoader {
  const val KEYSTORE_PATH = "keystore"
  private const val KEYS = "keys"
  const val STORE_PASS = "storePass"
  const val KEY_ALIAS = "keyAlias"
  const val KEY_PASS = "keyPass"

  private fun Properties.default() {
    this.setProperty(KEYSTORE_PATH, "placeholder")
    this.setProperty(STORE_PASS, "placeholder")
    this.setProperty(KEY_ALIAS, "placeholder")
    this.setProperty(KEY_PASS, "placeholder")
  }

  private val signing = File("signing.properties")

  private val properties by lazy {
    Properties().apply {
      if (signing.exists()) {
        val signProps = Properties().apply { load(FileInputStream(signing)) }
        val keyFile = File(signProps.getProperty(KEYS))
        if (keyFile.exists()) {
          load(FileInputStream(keyFile))
        } else {
          default()
        }
      } else {
        default()
      }
    }
  }

  fun getValue(key: String): String = properties.getProperty(key)

  fun fromEnvironment(): Boolean {
    return System.getenv("KEYSTORE_BASE64") != null || System.getenv("KEYSTORE_FILE") != null
  }

  fun keystoreFile(project: Project): File? {
    val base64 = System.getenv("KEYSTORE_BASE64")
    if (base64 != null) {
      val file = File(project.layout.buildDirectory.asFile.get(), "release.jks")
      file.parentFile.mkdirs()
      file.writeBytes(Base64.getDecoder().decode(base64))
      return file
    }
    val path = System.getenv("KEYSTORE_FILE")
    if (path != null) return File(path).takeIf { it.exists() }
    return null
  }

  fun isConfigured(): Boolean {
    if (fromEnvironment()) return true
    val keystorePath = getValue(KEYSTORE_PATH)
    return keystorePath != "placeholder" && File(keystorePath).exists()
  }
}

val gitHashProvider = providers.exec {
  workingDir = rootDir
  commandLine("git", "rev-parse", "--short", "HEAD")
  isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }.orElse("unknown")

val buildTimeProvider = providers.provider {
  val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
  df.timeZone = TimeZone.getTimeZone("UTC")
  df.format(Date())
}

val appVersionName = "1.6.1"
val appVersionCode = 131
val minSDKVersion = 23
val compileSDKVersion = 37
// targetSdk 37 opts into Android 17 behaviors (see ACCESS_LOCAL_NETWORK handling in MainActivity).
val targetSDKVersion = 37

android {
  compileSdk = compileSDKVersion
  namespace = "com.kelsos.mbrc"
  testNamespace = "com.kelsos.mbrc.test"

  experimentalProperties["android.experimental.enableScreenshotTest"] = true

  buildFeatures {
    buildConfig = true
    compose = true
  }

  defaultConfig {
    applicationId = "com.kelsos.mbrc"
    minSdk = minSDKVersion
    targetSdk = targetSDKVersion
    versionCode = appVersionCode
    versionName = appVersionName
    buildConfigField("String", "GIT_SHA", "\"${gitHashProvider.get()}\"")
    buildConfigField("String", "BUILD_TIME", "\"${buildTimeProvider.get()}\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // Explicit test applicationId to avoid namespace collision with debug .dev suffix
    testApplicationId = "com.kelsos.mbrc.test"
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    unitTests.isIncludeAndroidResources = true
  }

  // Configure screenshot tests to use less memory
  tasks.withType<Test>().matching { it.name.contains("ScreenshotTest") }.configureEach {
    maxHeapSize = "4g"
    maxParallelForks = 1
    forkEvery = 10 // Restart JVM every 10 tests to free memory
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
        "kotlinx.coroutines.ExperimentalCoroutinesApi",
        "kotlin.RequiresOptIn",
        "kotlin.time.ExperimentalTime",
      ))
    }
  }

  // Enable Compose compiler reports for debugging performance
  composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
  }

  signingConfigs {
    if (KeyLoader.fromEnvironment()) {
      create("release") {
        storeFile = KeyLoader.keystoreFile(project)
        keyAlias = System.getenv("KEY_ALIAS")
        storePassword = System.getenv("STORE_PASSWORD")
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    } else if (KeyLoader.isConfigured()) {
      create("release") {
        storeFile = file(KeyLoader.getValue(KeyLoader.KEYSTORE_PATH))
        keyAlias = KeyLoader.getValue(KeyLoader.KEY_ALIAS)
        storePassword = KeyLoader.getValue(KeyLoader.STORE_PASS)
        keyPassword = KeyLoader.getValue(KeyLoader.KEY_PASS)
      }
    }
  }

  buildTypes {
    val releaseSigningConfig = if (KeyLoader.isConfigured()) {
      signingConfigs.getByName("release")
    } else {
      signingConfigs.getByName("debug")
    }

    getByName("release") {
      signingConfig = releaseSigningConfig
      isDebuggable = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      buildConfigField("String", "GIT_SHA", "\"${gitHashProvider.get()}\"")
      buildConfigField("String", "BUILD_TIME", "\"${buildTimeProvider.get()}\"")
    }

    getByName("debug") {
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-dev"
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true

      buildConfigField("String", "GIT_SHA", "\"debug_build\"")
      buildConfigField("String", "BUILD_TIME", "\"debug_build\"")
    }

    // Baseline profile build types always use debug signing for local generation
    create("benchmarkRelease") {
      initWith(getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += "release"
      isDebuggable = false
    }

    create("nonMinifiedRelease") {
      initWith(getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += "release"
      isDebuggable = false
      isMinifyEnabled = false
    }
  }

  flavorDimensions.add("base")

  productFlavors {
    create("play") {
      apply(plugin = libs.plugins.googleServices.get().pluginId)
      apply(plugin = libs.plugins.crashlytics.get().pluginId)
    }

    create("github") {}
  }

  packaging {
    resources {
      excludes += "META-INF/ASL2.0"
      excludes += "META-INF/LICENSE"
      excludes += "META-INF/LICENSE.md"
      excludes += "META-INF/LICENSE-notice.md"
      excludes += "META-INF/NOTICE"
      excludes += "META-INF/services/javax.annotation.processing.Processor"
      excludes += "**/module-info.class"
      pickFirsts.add("META-INF/atomicfu.kotlin_module")
    }
  }

  sourceSets {
    getByName("androidTest") {
      assets.srcDirs("$projectDir/schemas")
    }
  }

  lint {
    lintConfig = rootProject.file("config/lint.xml")
    sarifReport = true
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

baselineProfile {
  dexLayoutOptimization = true
  mergeIntoMain = true
}

detekt {
  source.setFrom(files("src/main/java", "src/main/kotlin"))
  config.setFrom(files(rootProject.file("config/detekt/detekt.yml")))
  buildUponDefaultConfig = true
}

val dummyGoogleServicesJson: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false

  attributes {
    attribute(Attribute.of("google.services.json", String::class.java), "dummy-json")
  }
}

dependencies {
  coreLibraryDesugaring(libs.com.android.tools.desugar)

  implementation(project(":core:platform"))
  implementation(project(":core:networking"))
  implementation(project(":core:data"))
  implementation(project(":core:queue"))
  implementation(project(":core:ui"))

  implementation(project(":feature:library"))
  implementation(project(":feature:minicontrol"))
  implementation(project(":feature:playback"))
  implementation(project(":feature:widgets"))
  implementation(project(":feature:settings"))
  implementation(project(":feature:content"))
  implementation(project(":feature:misc"))

  implementation(libs.androidx.annotation)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.bundles.androidx.media3)
  implementation(libs.androidx.paging.runtime.ktx)
  implementation(libs.androidx.paging.compose)
  implementation(libs.androidx.preference.ktx)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.viewpager2)
  implementation(libs.androidx.work.ktx)
  implementation(libs.androidx.swiperefreshlayout)
  implementation(libs.bundles.androidx.lifecycle)
  implementation(libs.bundles.coroutines)
  implementation(libs.bundles.androidx.room)
  implementation(libs.bundles.coil)
  implementation(libs.bundles.koin)
  implementation(libs.google.material)
  implementation(libs.google.protobuf.javalite)
  implementation(libs.squareup.moshi.lib)
  implementation(libs.squareup.okio)
  implementation(libs.squareup.okhttp)
  implementation(libs.timber)

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.bundles.androidx.compose)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.paging.compose)
  implementation(libs.coilKt.compose)
  implementation(libs.androidx.palette)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.profileinstaller)

  baselineProfile(project(":baselineprofile"))

  ksp(libs.androidx.room.compiler)
  ksp(libs.squareup.moshi.codegen)

  testImplementation(testFixtures(project(":core:common")))
  testImplementation(libs.androidx.arch.core.testing)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.runner)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.androidx.test.truth)
  testImplementation(libs.bundles.androidx.test.espresso)
  testImplementation(libs.androidx.paging.common.ktx)
  testImplementation(libs.androidx.paging.testing)
  testImplementation(libs.turbine)
  testImplementation(libs.truth)
  testImplementation(libs.koin.test)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.glance.testing)
  testImplementation(libs.androidx.glance.appwidget.testing)

  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.junit)
  androidTestImplementation(libs.truth)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.koin.test)
  androidTestImplementation(libs.kotlin.coroutines.test)
  androidTestImplementation(libs.mockk.android)

  debugImplementation(libs.squareup.leakcanary)
  debugImplementation(libs.androidx.fragment.testing)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  screenshotTestImplementation(libs.screenshot.validation.api)
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)

  "playImplementation"(platform(libs.google.firebase.bom))
  "playImplementation"(libs.bundles.google.firebase) {
    exclude(group = "com.google.protobuf")
    exclude(group = "com.google.firebase", module = "protolite-well-known-types")
  }
  dummyGoogleServicesJson(projects.mbrc)
}

kover {
  reports {
    filters.excludes.androidGeneratedClasses()
  }
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${project.libs.versions.protobuf.get()}"
  }

  generateProtoTasks {
    all().forEach { task ->
      task.builtins {
        create("java") {
          option("lite")
        }
      }
    }
  }
}

abstract class GenerateGoogleServicesJson : DefaultTask() {

  @get:InputFiles
  abstract val inputFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val outputJson: RegularFileProperty

  @TaskAction
  fun generateJson() {
    outputJson.get().asFile.writeText(inputFiles.singleFile.readText())
  }
}

tasks {
  withType<Detekt> {
    jvmTarget = "11"
    reports {
      sarif {
        required.set(true)
      }
    }
  }

  val copyDummyGoogleServicesJson by registering(GenerateGoogleServicesJson::class) {
    val googleServicesFile = file("google-services.json")
    onlyIf {
      System.getenv("CI") == "true" && !googleServicesFile.exists()
    }
    doFirst {
      val gitRef = System.getenv("GITHUB_REF").orEmpty()
      if (gitRef.startsWith("refs/tags/")) {
        throw GradleException(
          "google-services.json is missing during a tag build. " +
            "Add the GOOGLE_SERVICES_JSON secret to the release environment.",
        )
      }
    }
    inputFiles.from(dummyGoogleServicesJson)
    outputJson.set(file("google-services.json"))
  }

  val checkGoogleServicesJson by registering {
    val googleServicesFile = file("google-services.json")
    onlyIf { System.getenv("CI") != "true" }
    doLast {
      if (!googleServicesFile.exists()) {
        throw GradleException(
          "You need a google-services.json file to run this project." +
            " Please refer to the CONTRIBUTING.md file for details."
        )
      }
    }
  }

  val copyLicenseToAssets = project.tasks.register<CopyLicenseTask>("copyLicenseToAssets") {
    licenseFile.set(rootProject.file("LICENSE"))
  }

  androidComponents {
    onVariants { variant ->
      variant.sources.assets?.addGeneratedSourceDirectory(
        copyLicenseToAssets,
        CopyLicenseTask::outputDir,
      )
    }
  }

  afterEvaluate {
    // Set up Google Services task dependencies for all variants
    listOf(
      "processGithubDebugGoogleServices",
      "processGithubReleaseGoogleServices",
      "processGithubBenchmarkReleaseGoogleServices",
      "processGithubNonMinifiedReleaseGoogleServices",
      "processPlayDebugGoogleServices",
      "processPlayReleaseGoogleServices",
      "processPlayBenchmarkReleaseGoogleServices",
      "processPlayNonMinifiedReleaseGoogleServices",
    ).forEach { taskName ->
      tasks.findByName(taskName)?.dependsOn(copyDummyGoogleServicesJson, checkGoogleServicesJson)
    }
  }
}

configurations.all {
  resolutionStrategy {
    force("com.google.code.findbugs:jsr305:3.0.2")
    force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.libs.versions.kotlin.get()}")
    force("org.jetbrains.kotlin:kotlin-reflect:${project.libs.versions.kotlin.get()}")
    // Room 2.8.x requires kotlinx-serialization 1.8.x for migration testing
    force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
  }
}


fun String.toKebabCase(): String {
  return this.trim()
    .replace(Regex("([a-z])([A-Z])"), "$1-$2")
    .replace(Regex("[\\s_]+"), "-")
    .replace(Regex("[^a-zA-Z0-9-]"), "")
    .lowercase()
}

fun String.prefixIfNot(prefix: String): String = if (this.startsWith(prefix)) this else "$prefix-$this"

abstract class CopyLicenseTask : DefaultTask() {
  @get:InputFile
  abstract val licenseFile: RegularFileProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun copy() {
    val dir = outputDir.get().asFile
    dir.mkdirs()
    licenseFile.get().asFile.copyTo(dir.resolve("LICENSE.txt"), overwrite = true)
  }
}
