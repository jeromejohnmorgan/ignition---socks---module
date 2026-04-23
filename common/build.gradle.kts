plugins {
    `java-library`
}

val ignitionSdkVersion: String by rootProject.extra
val javaTargetVersion: String by rootProject.extra

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaTargetVersion.toInt()))
    }
}

dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:$ignitionSdkVersion")
    compileOnly("com.inductiveautomation.ignitionsdk:perspective-common:$ignitionSdkVersion")
    compileOnly(libs.google.guava)
    compileOnly(libs.ia.gson)
}
