@file:Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Aliyun mirrors as fallback (useful in CN networks)
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

rootProject.name = "JetMelo"
include(":app")
include(":ncmapi")
