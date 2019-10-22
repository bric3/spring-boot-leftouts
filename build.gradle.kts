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
    implementation("org.springframework:spring-beans:5.2.0.RELEASE")
    implementation("org.springframework:spring-context:5.2.0.RELEASE")
    implementation("org.springframework.boot:spring-boot-autoconfigure:2.2.0.RELEASE")

    testImplementation("org.springframework:spring-test:5.2.0.RELEASE")
    testImplementation("org.springframework.boot:spring-boot-test:2.2.0.RELEASE")
    testImplementation("org.assertj:assertj-core:3.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
