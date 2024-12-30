import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.android.build.gradle.internal.lint.AndroidLintTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.gitlab.arturbosch.detekt.Detekt
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinParcelize)
  alias(libs.plugins.kapt)
  alias(libs.plugins.protobuf)
  alias(libs.plugins.googleServices) apply false
  alias(libs.plugins.crashlytics) apply false
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.kover)
  alias(libs.plugins.detekt)
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
}

fun String.runCommand(currentWorkingDir: File = file("./")): String {
  val byteOut = ByteArrayOutputStream()
  project.exec {
    workingDir = currentWorkingDir
    commandLine = this@runCommand.split("\\s".toRegex())
    standardOutput = byteOut
  }
  return String(byteOut.toByteArray()).trim()
}


fun buildTime(): String {
  val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
  df.timeZone = TimeZone.getTimeZone("UTC")
  return df.format(Date())
}

fun gitHash(): String {
  return "git -C $rootDir rev-parse --short HEAD".runCommand()
}

val version = "1.6.0-alpha.1"
val code = 125

android {
  compileSdk = 35
  namespace = "com.kelsos.mbrc"

  buildFeatures {
    buildConfig = true
  }

  defaultConfig {
    applicationId = "com.kelsos.mbrc"
    minSdk = 23
    targetSdk = 35
    versionCode = code
    versionName = version
    buildConfigField("String", "GIT_SHA", "\"${gitHash()}\"")
    buildConfigField("String", "BUILD_TIME", "\"${buildTime()}\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    unitTests.isIncludeAndroidResources = true
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs = listOf(
      "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
      "-opt-in=kotlin.RequiresOptIn",
      "-opt-in=kotlin.time.ExperimentalTime",
    )
  }

  signingConfigs {
    create("release") {
      storeFile = file(KeyLoader.getValue(KeyLoader.KEYSTORE_PATH))
      keyAlias = KeyLoader.getValue(KeyLoader.KEY_ALIAS)
      storePassword = KeyLoader.getValue(KeyLoader.STORE_PASS)
      keyPassword = KeyLoader.getValue(KeyLoader.KEY_PASS)
    }
  }

  buildTypes {
    getByName("release") {
      signingConfig = signingConfigs.getByName("release")
      isDebuggable = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      buildConfigField("String", "GIT_SHA", "\"${gitHash()}\"")
      buildConfigField("String", "BUILD_TIME", "\"${buildTime()}\"")
    }

    getByName("debug") {
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-dev"
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true

      buildConfigField("String", "GIT_SHA", "\"debug_build\"")
      buildConfigField("String", "BUILD_TIME", "\"debug_build\"")
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
      excludes += "META-INF/NOTICE"
      excludes += "META-INF/services/javax.annotation.processing.Processor"
      excludes += "**/module-info.class"
      pickFirsts.add("META-INF/atomicfu.kotlin_module")
    }
  }

  lint {
    lintConfig = rootProject.file("config/lint.xml")
    sarifReport = true
  }

  applicationVariants.all {
    val variant = this
    variant.outputs.map { it as BaseVariantOutputImpl }
      .forEach { output ->
        val applicationId = defaultConfig.applicationId
        val flavorName = variant.flavorName
        val name = "$applicationId-$flavorName-${variant.versionCode}-v${variant.versionName}.apk"
        output.outputFileName = name
      }
  }
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

  implementation(projects.changelog)

  testImplementation(libs.toothpick.testing)
  testImplementation(libs.androidx.arch.core.testing)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.runner)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.androidx.test.truth)
  testImplementation(libs.bundles.androidx.test.espresso)
  testImplementation(libs.truth)
  testImplementation(libs.kotlin.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)
  testImplementation(libs.threetenbp)

  implementation(libs.androidx.annotation)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.media)
  implementation(libs.androidx.paging.runtime.ktx)
  implementation(libs.androidx.preference.ktx)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.viewpager2)
  implementation(libs.bundles.androidx.lifecycle)
  implementation(libs.androidx.legacy.support.v4)
  implementation(libs.androidx.legacy.support.v13)
  implementation(libs.bundles.coroutines)
  implementation(libs.google.material)
  implementation(libs.google.protobuf.javalite)
  implementation(libs.squareup.okio)
  implementation(libs.timber)

  implementation(libs.bundles.dbflow)
  implementation(libs.bundles.jackson)
  implementation(libs.bundles.toothpick)
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlin.reflect)
  implementation(libs.squareup.picasso)
  implementation(libs.rxandroid)
  implementation(libs.rxjava)
  implementation(libs.rxkotlin)
  implementation(libs.rxrelay)
  implementation(libs.threetenabp)

  kapt(libs.dbflow.processor)
  kapt(libs.toothpick.compiler)

  debugImplementation(libs.squareup.leakcanary)
  debugImplementation(libs.androidx.fragment.testing)

  "playImplementation"(platform(libs.google.firebase.bom))
  "playImplementation"(libs.bundles.google.firebase)
  dummyGoogleServicesJson(projects.mbrc)
}


dependencies {
  kover(project(":changelog"))
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

open class GenerateGoogleServicesJson : DefaultTask() {

  @get:InputFiles
  var configuration by project.objects.property<Configuration>()

  @get:OutputFile
  var outputJson by project.objects.property<File>()

  @TaskAction
  fun generateJson() {
    outputJson.writeText(configuration.resolve().single().readText())
  }
}

tasks {
  val detekt = withType<Detekt> {
    jvmTarget = "11"
    reports {
      sarif {
        required.set(true)
      }
    }
  }

  val lintReportReleaseSarifOutput = project.layout.buildDirectory.file("reports/sarif/lint-results-release.sarif")

  afterEvaluate {
    named<AndroidLintTask>("lintReportGithubRelease") {
      sarifReportOutputFile.set(lintReportReleaseSarifOutput)
    }

    val staticAnalysis by registering {
      val detektRelease by named<Detekt>("detektGithubRelease")
      val androidLintReportRelease = named<AndroidLintTask>("lintReportGithubRelease")

      dependsOn(detekt, detektRelease, androidLintReportRelease, lintKotlin)
    }

    register<Sync>("collectSarifReports") {
      val detektRelease by named<Detekt>("detektGithubRelease")
      val androidLintReportRelease = named<AndroidLintTask>("lintReportGithubRelease")

      mustRunAfter(detekt, detektRelease, androidLintReportRelease, lintKotlin, staticAnalysis)

      from(detektRelease.sarifReportFile) {
        rename { "detekt-release.sarif" }
      }
      detekt.forEach {
        from(it.sarifReportFile) {
          rename { "detekt.sarif" }
        }
      }
      from(lintReportReleaseSarifOutput) {
        rename { "android-lint.sarif" }
      }

      into(layout.buildDirectory.dir("reports/sarif"))

      doLast {
        logger.info("Copied ${inputs.files.files.filter { it.exists() }} into ${outputs.files.files}")
        logger.info("Output dir contents:\n${outputs.files.files.first().listFiles()?.joinToString()}")
      }
    }
  }



  val copyDummyGoogleServicesJson by registering(GenerateGoogleServicesJson::class) {
    onlyIf { System.getenv("CI") == "true" }
    configuration = dummyGoogleServicesJson
    outputJson = file("google-services.json")
  }

  val checkGoogleServicesJson by registering {
    onlyIf { System.getenv("CI") != "true" }
    doLast {
      if (!project.file("google-services.json").exists()) {
        throw GradleException(
          "You need a google-services.json file to run this project. Please refer to the CONTRIBUTING.md file for details."
        )
      }
    }
  }

  afterEvaluate {
    named("processGithubReleaseGoogleServices").dependsOn(copyDummyGoogleServicesJson, checkGoogleServicesJson)
    named("processPlayReleaseGoogleServices").dependsOn(copyDummyGoogleServicesJson, checkGoogleServicesJson)
    named("processGithubDebugGoogleServices").dependsOn(copyDummyGoogleServicesJson, checkGoogleServicesJson)
    named("processPlayDebugGoogleServices").dependsOn(copyDummyGoogleServicesJson, checkGoogleServicesJson)
  }
}

configurations.all {
  resolutionStrategy {
    force("com.google.code.findbugs:jsr305:3.0.2")
    force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.libs.versions.kotlin.get()}")
    force("org.jetbrains.kotlin:kotlin-reflect:${project.libs.versions.kotlin.get()}")
  }
}

kapt {
  arguments {
    arg("toothpick_registry_package_name", *arrayOf("com.kelsos.mbrc").map { it }.toTypedArray())
  }
}
