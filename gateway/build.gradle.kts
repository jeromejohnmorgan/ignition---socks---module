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
    implementation(projects.common)
    modlImplementation(projects.web)

    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:$ignitionSdkVersion")
    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:$ignitionSdkVersion")
    implementation("com.inductiveautomation.ignitionsdk:perspective-gateway:$ignitionSdkVersion")
    implementation("com.inductiveautomation.ignitionsdk:perspective-common:$ignitionSdkVersion")
    compileOnly(libs.ia.gson)
}
