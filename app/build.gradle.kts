plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.eodumneomeo.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.eodumneomeo.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    // 샘플 DB, 시나리오, 안내 설정을 core 테스트와 같은 파일로 쓴다
    sourceSets["main"].assets.srcDir("../data")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    // Pleos SDK(예: ai.pleos.playground:Vehicle)는 API 확인 후 연결한다. docs/sdk-status.md 참고
}
