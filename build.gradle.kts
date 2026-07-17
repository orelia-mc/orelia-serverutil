plugins {
    id("java")
}

subprojects {
    apply(plugin = "java")

    group = "rpg"
    version = "1.0.0"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        // TEMP DEV LOOP: prefers a locally-published orelia-core (./gradlew publishToMavenLocal
        // in that repo) over jitpack, so in-flight core changes are picked up without a push.
        // Remove this line before merging - production builds should resolve from jitpack only.
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        // Resolves orelia-core straight from its GitHub repo (softdepend, StatusApi/EconomyApi
        // reference only), same convention as orelia-world/orelia-extra/orelia-debug.
        maven("https://jitpack.io")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
