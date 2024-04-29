import groovy.json.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.util.jar.*
import java.util.zip.Deflater

plugins {
    java
    idea
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

buildscript {
    repositories.maven("https://oss.sonatype.org/content/repositories/releases")
    dependencies.classpath("org.ow2.asm:asm-tree:9.7")
}

group = "maven_group"()
version = "app_version"()

base.archivesName = "archives_base_name"()

val includeMac: Configuration by configurations.creating

repositories {
    mavenCentral()
}

val isMac = System.getProperty("os.name").lowercase().contains("mac")

dependencies {
    // test runtime dependency on the old version so we can see it but cant reference it
    testRuntimeOnly(files("libs/orbital-old.jar"))

    implementation("org.joml:joml:${"joml_version"()}")!!.also {
        shadow(it)
        includeMac(it)
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

    implementation("blue.endless:jankson:${"jankson_version"()}")!!.also {
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
    dependsOn(tasks.compileJava, tasks.processResources)
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

    doLast {
        advStrip(archiveFile.get().asFile)
    }
}

tasks.register<JavaExec>("testRun") {
    dependsOn(tasks.compileJava, tasks.processResources)
    mainClass = "samuschair.orbital2.Test"
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

tasks.shadowJar {
    from(tasks.jar.get())
    exclude {
        it.path.startsWith("META-INF/") && it.path != "META-INF/MANIFEST.MF"
    }

    archiveClassifier = "all"

    doLast {
        advStrip(archiveFile.get().asFile)
    }
}

tasks.register("macApp") {
    dependsOn(tasks.jar.get())
    doLast {
        makeApp()
    }
}

fun advzip(zip: File, level: Int = 3, iterations: Int? = null) {
    val args = mutableListOf("advzip", "-z", "-$level", "-q", zip.absolutePath)
    if (iterations != null) {
        if(iterations < 1) throw IllegalArgumentException("Iterations must be at least 1")
        if(level != 4) throw IllegalArgumentException("Iterations are only supported for level 4")
        args.add(iterations.toString())
    }

    exec { commandLine(args) }
}

fun stripJar(jar: File) {
    val contents = linkedMapOf<String, ByteArray>()
    JarFile(jar).use {
        it.entries().asIterator().forEach { entry ->
            if (!entry.isDirectory) {
                contents[entry.name] = it.getInputStream(entry).readAllBytes()
            }
        }
    }

    jar.delete()

    JarOutputStream(jar.outputStream()).use { out ->
        out.setLevel(Deflater.BEST_COMPRESSION)
        contents.forEach { var (name, bytes) = it
            if (name.endsWith(".json")) {
                bytes = JsonOutput.toJson(JsonSlurper().parse(bytes)).toByteArray()
            }

            if (name.endsWith(".class")) {
                val reader = ClassReader(bytes)
                val node = ClassNode()
                reader.accept(node, 0)

                node.methods.forEach { method ->
                    method.localVariables?.clear()
                    method.parameters?.clear()
                }
                if ("strip_source_files"().toBoolean()) {
                    node.sourceFile = null
                }

                val writer = ClassWriter(0)
                node.accept(writer)
                bytes = writer.toByteArray()
            }

            out.putNextEntry(JarEntry(name))
            out.write(bytes)
            out.closeEntry()
        }
        out.finish()
        out.close()
    }
}

fun advStrip(input: Any) {
    val jar: File = when (input) {
        is Jar -> input.archiveFile.get().asFile
        is File -> input
        else -> throw IllegalArgumentException("Input must be a Jar or File")
    }
    stripJar(jar)
    advzip(jar)
}

fun makeApp() {
    val appDir = projectDir.resolve("build/app")
    appDir.mkdirs()

    val app = appDir.resolve("${project.name}.app")
    if(app.exists()) {
        app.deleteRecursively()
    }
    app.mkdirs()

    val contents = app.resolve("Contents")
    contents.mkdirs()

    val macos = contents.resolve("MacOS")
    macos.mkdirs()

    val java = contents.resolve("Java")
    java.mkdirs()

    val infoPlist = contents.resolve("Info.plist")
    infoPlist.writeText("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "https://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleExecutable</key>
                <string>launcher</string>
                <key>CFBundleIconFile</key>
                <string>GenericApp.icns</string>
                <key>CFBundleIdentifier</key>
                <string>${group}.${base.archivesName.get()}</string>
                <key>CFBundleName</key>
                <string>${project.name}</string>
                <key>CFBundlePackageType</key>
                <string>APPL</string>
                <key>CFBundleShortVersionString</key>
                <string>${version}</string>
                <key>CFBundleVersion</key>
                <string>${version}</string>
                <key>LSMinimumSystemVersion</key>
                <string>10.14</string>
            </dict>
            </plist>
        """.trimIndent())

    tasks.jar.get().archiveFile.get().asFile.also {
        it.copyTo(java.resolve(it.name), overwrite = true)
    }

    val script = macos.resolve("launcher")
    script.writeText("""
            #!/bin/bash
            cd "$(dirname "${'$'}0")/../Java"
            JARS=$(ls *.jar)
            CP=""
            for JAR in ${'$'}JARS; do
              CP="${'$'}CP:${'$'}JAR"
            done
            java -cp ${'$'}CP -XstartOnFirstThread -Xms2G -Xmx2G ${tasks.jar.get().manifest.attributes["Main-Class"]}
        """.trimIndent())

    script.setExecutable(true)

    includeMac.files.forEach {
        it.copyTo(java.resolve(it.name), overwrite = true).also(::advStrip)
    }

    exec {
        commandLine("/usr/bin/SetFile", "-a", "B", app.absolutePath)
    }
}

tasks.assemble {
    dependsOn(tasks["macApp"], tasks.shadowJar)
}

operator fun String.invoke(): String = rootProject.ext[this] as? String
        ?: throw IllegalStateException("Property $this is not defined")