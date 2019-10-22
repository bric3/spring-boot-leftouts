plugins {
    `java-library`
}

repositories {
    jcenter()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.springframework:spring-beans:4.3.25.RELEASE")
    implementation("org.springframework:spring-context:4.3.25.RELEASE")

    testImplementation("org.springframework:spring-test:4.3.25.RELEASE")
    testImplementation("org.assertj:assertj-core:3.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testCompileOnly("junit:junit:4.12") {
        because("Because we want to migrate to JUnit 5 progressively")
    }
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.4.2") {
        because("Because we want to migrate to JUnit 5 progressively")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
