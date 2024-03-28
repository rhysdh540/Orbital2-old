plugins {
    java
    idea
    id("edu.sc.seis.macAppBundle") version "2.3.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "maven_group"()
version = "app_version"()

base.archivesName = "archives_base_name"()

configurations.default.get().isCanBeResolved = true

repositories {
    mavenCentral()
}

macAppBundle {
    mainClassName = "samuschair.orbital2.Orbital2Main"
    bundleJRE = true
    javaProperties["apple.laf.useScreenMenuBar"] = "true"
    javaExtras["-XstartOnFirstThread"] = null
}

dependencies {
    // test runtime dependency on the old version so we can see it but cant reference it
    testRuntimeOnly(files("libs/orbital-old.jar"))

//    shadow(implementation(("org.joml:joml:${"joml_version"()}"))!!)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    val lwjglVersion = "lwjgl_version"()
    val nativesPlatforms = "lwjgl_natives"().split(", ")
    val modules = "lwjgl_modules"().split(", ")

    shadow(implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))!!)
    shadow(implementation("org.lwjgl:lwjgl")!!)
    for(platform in nativesPlatforms) {
        shadow(runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-$platform")!!)
    }
    for(module in modules) {
        shadow(implementation("org.lwjgl:lwjgl-$module")!!)
        for(platform in nativesPlatforms) {
            shadow(runtimeOnly("org.lwjgl:lwjgl-$module:$lwjglVersion:natives-$platform")!!)
        }
    }
}

tasks.register<JavaExec>("run") {
    dependsOn(tasks.jar)
    mainClass = "samuschair.orbital2.Orbital2Main"
    classpath(
            configurations.runtimeClasspath.get(),
            tasks.compileJava.get().destinationDirectory,
            tasks.processResources.get().destinationDir
    )
    jvmArgs = listOf("-XstartOnFirstThread")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "samuschair.orbital2.Orbital2Main"
    }
}

tasks.shadowJar {
    from(tasks.jar.get())
    exclude {
        it.path.startsWith("META-INF/") && it.path != "META-INF/MANIFEST.MF"
    }

    archiveClassifier = "all"
}

tasks.assemble {
    dependsOn(tasks.shadowJar.get())
}

operator fun String.invoke(): String = rootProject.ext[this] as? String
        ?: throw IllegalStateException("Property $this is not defined")