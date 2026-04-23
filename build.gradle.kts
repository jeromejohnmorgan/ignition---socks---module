import java.util.concurrent.TimeUnit

plugins {
    base
    id("io.ia.sdk.modl") version("0.1.1")
    id("org.barfuin.gradle.taskinfo") version "1.3.0"
}

// ── Dual-versioning properties ────────────────────────────────────────────────
// Defaults come from gradle.properties (Ignition 8.3 / Java 17).
// Override for an 8.1 build:
//   ./gradlew buildModule \
//     -PignitionSdkVersion=8.1.43 \
//     -PignitionMinVersion=8.1.0  \
//     -PjavaTargetVersion=11
val ignitionSdkVersion: String by project
val ignitionMinVersion: String by project
val javaTargetVersion: String by project

allprojects {
    version = "__MODULE_VERSION__"
    group = "com.kyvislabs"

    // Propagate to subprojects so their build.gradle.kts files can read them.
    extra["ignitionSdkVersion"] = ignitionSdkVersion
    extra["javaTargetVersion"]  = javaTargetVersion
}

ignitionModule {
    fileName.set("gsap-perspective")
    name.set("GSAP Perspective Components")
    id.set("com.kyvislabs.gsap")
    moduleVersion.set("${project.version}")
    moduleDescription.set("High-performance GSAP animations for Ignition Perspective dashboards")
    requiredIgnitionVersion.set(ignitionMinVersion)
    license.set("license.html")

    moduleDependencies.put("com.inductiveautomation.perspective", "DG")

    projectScopes.putAll(
        mapOf(
            ":gateway"  to "G",
            ":web"      to "G",
            ":designer" to "D",
            ":common"   to "GD"
        )
    )

    hooks.putAll(
        mapOf(
            "com.kyvislabs.gsap.gateway.GatewayHook"  to "G",
            "com.kyvislabs.gsap.designer.DesignerHook" to "D"
        )
    )
}

val deepClean by tasks.registering {
    dependsOn(allprojects.map { "${it.path}:clean" })
    description = "Executes clean tasks and removes node plugin caches."
    doLast {
        delete(file(".gradle"))
    }
}
