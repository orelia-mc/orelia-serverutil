plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.velocitypowered:velocity-api:3.5.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.1")
    // Velocity itself bundles these at runtime, so they're compileOnly here (not shaded).
    compileOnly("org.yaml:snakeyaml:2.5")
    compileOnly("com.google.code.gson:gson:2.13.2")
}

tasks {
    shadowJar {
        archiveBaseName.set("orelia-serverutil-velocity")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
