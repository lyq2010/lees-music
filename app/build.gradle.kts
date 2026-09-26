import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lyq2010.leesmusic"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lyq2010.leesmusic"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.1.2"
        buildConfigField("String", "COS_UPDATE_BASE", "\"https://releases.angelolee.cn\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The app UI is Chinese; retain English fallback for AndroidX controls.
    androidResources { localeFilters += listOf("zh", "en") }

    signingConfigs {
        create("distribution") {
            val path = providers.environmentVariable("LEES_MUSIC_KEYSTORE").orNull
            if (path != null) {
                storeFile = file(path)
                storePassword = providers.environmentVariable("LEES_MUSIC_STORE_PASSWORD").get()
                keyAlias = "lees-music"
                keyPassword = providers.environmentVariable("LEES_MUSIC_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        release {
            optimization { enable = true }
            signingConfig = signingConfigs.getByName("distribution")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

abstract class LegalAssetsTask : DefaultTask() {
    @get:InputFiles abstract val documents: ConfigurableFileCollection
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @TaskAction fun copyDocuments() {
        val directory = outputDirectory.get().asFile.apply { mkdirs() }
        documents.forEach { it.copyTo(directory.resolve(it.name), overwrite = true) }
    }
}
val copyLegalAssets = tasks.register<LegalAssetsTask>("copyLegalAssets") {
    documents.from(rootProject.files("LICENSE", "docs/THIRD_PARTY_NOTICES.md", "docs/APP_CHANGELOG.md"))
    outputDirectory.set(layout.buildDirectory.dir("generated/legal-assets"))
}
androidComponents.onVariants { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(copyLegalAssets, LegalAssetsTask::outputDirectory)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation("androidx.media3:media3-datasource-okhttp:1.11.1")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation("junit:junit:4.13.2")
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("releaseDependencyInventory") {
    doLast {
        val output = layout.buildDirectory.file("reports/release-dependencies.tsv").get().asFile
        output.parentFile.mkdirs()
        output.writeText(configurations.getByName("releaseRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
            .sortedBy { it.moduleVersion.id.toString() }.joinToString("\n") {
                "${it.moduleVersion.id}\t${it.file.absolutePath}"
            } + "\n")
    }
}
