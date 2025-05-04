plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.eclipse.paho/org.eclipse.paho.client.mqttv3
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    // https://mvnrepository.com/artifact/org.jason-lang/jason
    implementation("io.github.jason-lang:interpreter:3.2.0")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.1")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val mas2jFiles = file("src").walkTopDown()
    .filter { it.extension == "mas2j" }
    .toList()

if (mas2jFiles.isEmpty()) {
    println("⚠️ Nessun file MAS2J trovato in app/src/main/jason!")
} else {
    mas2jFiles.forEach { mas2jFile ->
        tasks.register<JavaExec>("run${mas2jFile.nameWithoutExtension}MAS") {
            group = "run"
            description = "Esegue il MAS ${mas2jFile.name}"

            classpath = sourceSets["main"].runtimeClasspath
            mainClass.set("jason.infra.centralised.RunCentralisedMAS")
            args(mas2jFile.absolutePath)

            standardInput = System.`in`

            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
            })
        }
    }
}

tasks.test {
    useJUnitPlatform()
}