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
    // Soft dependency only - see rpg.serverutil.paper.placeholder.PlaceholderApiHook, the only
    // class allowed to reference this directly (isolated so a missing PlaceholderAPI jar at
    // runtime never triggers NoClassDefFoundError anywhere else).
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // paper-api is compileOnly (provided by the server at runtime), but classes that touch it
    // (e.g. ColorUtil returning an Adventure Component) still need it resolvable when JUnit
    // loads/verifies the class under test - without this, tests fail with NoClassDefFoundError
    // even for methods that don't call any Adventure/Bukkit API themselves.
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
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
