plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

dependencies {
    implementation(project(":common"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // Soft dependency only - StatusApi/EconomyApi are used opportunistically when OreliaCore
    // happens to be installed (see rpg.serverutil.paper.integration.CoreIntegrationModule).
    // Every ServicesManager lookup must null-guard; this plugin must start fine without it.
    compileOnly("com.github.orelia-mc:orelia-core:main-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    shadowJar {
        archiveBaseName.set("orelia-serverutil")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
    test {
        useJUnitPlatform()
    }
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
