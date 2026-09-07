import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localProperty(name: String): String? = localProperties.getProperty(name)

android {
    namespace = "com.rcmiku.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rcmiku.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        localProperty("RELEASE_STORE_FILE")?.takeIf { it.isNotBlank() }?.let { releaseStoreFile ->
            register("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = localProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperty("RELEASE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/*.version", // https://stackoverflow.com/a/58956288
                "META-INF/**/LICENSE.txt",
                "DebugProbesKt.bin", // https://github.com/Kotlin/kotlinx.coroutines?tab=readme-ov-file#avoiding-including-the-debug-infrastructure-in-the-resulting-apk
                "kotlin-tooling-metadata.json"
            )
            pickFirsts += "META-INF/androidx.compose.ui_ui.version" // For Layout Inspector
        }
    }

    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") {
                    option("lite")
                }
                register("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.datastore)
    implementation(libs.datastore.preferences)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.media3)
    implementation(libs.media3.database)
    implementation(libs.media3.datasource)
    implementation(libs.media3.session)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.ktor.serialization.json)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.reorderable)
    implementation(project(":ncmapi"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
