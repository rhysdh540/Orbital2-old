plugins {
    java
    idea
}

group = "maven_group"()
version = "app_version"()

base.archivesName = "archives_base_name"()

repositories {
    mavenCentral()
}

val include: Configuration by configurations.creating

dependencies {
    // runtime dependency on the old version so we can see it but cant reference it
    runtimeOnly(files("libs/orbital-old.jar"))

    include(implementation(("org.joml:joml:${"joml_version"()}"))!!)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    val lwjglVersion = "lwjgl_version"()
    val nativesPlatforms = "lwjgl_natives"().split(", ")
    val modules = "lwjgl_modules"().split(", ")

    include(implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))!!)
    include(implementation("org.lwjgl:lwjgl")!!)
    for(platform in nativesPlatforms) {
        include(runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-$platform")!!)
    }
    for(module in modules) {
        include(implementation("org.lwjgl:lwjgl-$module")!!)
        for(platform in nativesPlatforms) {
            include(runtimeOnly("org.lwjgl:lwjgl-$module:$lwjglVersion:natives-$platform")!!)
        }
    }
}

tasks.register<JavaExec>("run") {
    dependsOn(tasks.jar)
    mainClass = "samuschair.orbital2.Orbital2Main"
    classpath(configurations.runtimeClasspath.get(), include, tasks.jar.get())
    jvmArgs = listOf("-Dorg.lwjgl.util.Debug=true", "-XstartOnFirstThread")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "samuschair.orbital2.Orbital2Main"
    }

    val excludes = listOf(
            "maven",
            "versions",
            "native-image",
            "windows", "macos", "linux",
    )

    from(include.map {
        for(exclude in excludes) {
            if(it.toString().startsWith("META-INF/$exclude")) {
                return@map null
            }
        }

        if (it.isDirectory)
            it
        else
            zipTree(it)
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

operator fun String.invoke(): String = rootProject.ext[this] as? String
        ?: throw IllegalStateException("Property $this is not defined")