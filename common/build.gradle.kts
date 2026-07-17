plugins {
    id("java")
}

dependencies {
    compileOnly("com.google.guava:guava:33.3.1-jre")
    testImplementation("com.google.guava:guava:33.3.1-jre")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
