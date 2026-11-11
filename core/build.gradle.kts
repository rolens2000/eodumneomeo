plugins { id("org.jetbrains.kotlin.jvm") }

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    // 테스트가 저장소 루트의 data/ 폴더를 읽는다.
    systemProperty("dataDir", rootProject.file("data").absolutePath)
    testLogging { events("passed", "failed", "skipped"); showStandardStreams = false }
}
