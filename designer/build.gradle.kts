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
    api(projects.common)
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:$ignitionSdkVersion")
    compileOnly(libs.google.jsr305)
    compileOnly("com.inductiveautomation.ignitionsdk:designer-api:$ignitionSdkVersion")
    compileOnly("com.inductiveautomation.ignitionsdk:perspective-common:$ignitionSdkVersion")
    compileOnly("com.inductiveautomation.ignitionsdk:perspective-designer:$ignitionSdkVersion")
}
