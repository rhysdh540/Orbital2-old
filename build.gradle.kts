import groovy.json.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.util.jar.*
import java.util.zip.Deflater

plugins {
    java
    idea
    id("edu.sc.seis.macAppBundle") version "2.3.1" apply(System.getProperty("os.name").lowercase().contains("mac"))
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

if(isMac) {
    the<edu.sc.seis.macAppBundle.MacAppBundlePluginExtension>().apply {
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
}

tasks.shadowJar {
    from(tasks.jar.get())
    exclude {
        it.path.startsWith("META-INF/") && it.path != "META-INF/MANIFEST.MF"
    }

    archiveClassifier = "all"

    doLast {
        val jar = archiveFile.get().asFile
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
}

tasks.assemble {
    dependsOn(tasks.shadowJar.get())
}

operator fun String.invoke(): String = rootProject.ext[this] as? String
        ?: throw IllegalStateException("Property $this is not defined")