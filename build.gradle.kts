import edu.sc.seis.macAppBundle.MacAppBundlePluginExtension

plugins {
    java
    idea
    id("edu.sc.seis.macAppBundle") version "2.3.1" apply(System.getProperty("os.name").lowercase().contains("mac"))
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "maven_group"()
version = "app_version"()

base.archivesName = "archives_base_name"()

val includeMac: Configuration by configurations.creating

repositories {
    mavenCentral()
}

val isMac = System.getProperty("os.name").lowercase().contains("mac")

if(isMac) {
    the<MacAppBundlePluginExtension>().apply {
        mainClassName = "samuschair.orbital2.Orbital2Main"
        javaExtras["-XstartOnFirstThread"] = null
        runtimeConfigurationName = includeMac.name
        jreHome = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(17) }.get().executablePath.asFile
                .parentFile // bin
                .parentFile // home
                .toString()
        bundleIdentifier = "$group.${base.archivesName}"
    }
}

dependencies {
    // test runtime dependency on the old version so we can see it but cant reference it
    testRuntimeOnly(files("libs/orbital-old.jar"))

    implementation("org.joml:joml:${"joml_version"()}")!!.also {
        shadow(it)
    }

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    compileOnly("org.projectlombok:lombok:${"lombok_version"()}")!!.also {
        annotationProcessor(it)
    }

    val lwjglVersion = "lwjgl_version"()
    val nativesPlatforms = "lwjgl_natives"().split(", ")
    val modules = "lwjgl_modules"().split(", ")

    platform("org.lwjgl:lwjgl-bom:$lwjglVersion").also {
        implementation(it)
        shadow(it)
        includeMac(it)
    }

    implementation("org.lwjgl:lwjgl")!!.also {
        shadow(it)
        includeMac(it)
    }

    for(platform in nativesPlatforms) {
        runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-$platform")!!.also {
            shadow(it)
            if(platform.startsWith("macos")) {
                includeMac(it)
            }
        }
    }

    for(module in modules) {
        implementation("org.lwjgl:lwjgl-$module")!!.also {
            shadow(it)
            includeMac(it)
        }
        for(platform in nativesPlatforms) {
            runtimeOnly("org.lwjgl:lwjgl-$module:$lwjglVersion:natives-$platform")!!.also {
                shadow(it)
                if(platform.startsWith("macos")) {
                    includeMac(it)
                }
            }
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
    jvmArgs("-Xmx2G", "-Dorbital.debug=true")
    if(isMac) {
        jvmArgs("-XstartOnFirstThread")
    }
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