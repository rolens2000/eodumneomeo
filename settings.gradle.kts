pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.24"
        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("com.android.application") version "8.5.2"
    }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}

rootProject.name = "eodumneomeo"
include(":core")

// app 모듈은 Android SDK가 있을 때만 포함한다. core 단위 테스트는 SDK 없이 JVM에서 실행된다.
val localProps = file("local.properties")
val hasAndroidSdk = System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null ||
    (localProps.exists() && localProps.readText().contains("sdk.dir"))
if (hasAndroidSdk) include(":app")
