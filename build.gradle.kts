plugins {
    java
    idea
}

group = "samuschair"
version = "0.1-dev"

base.archivesName = "orbital2"

repositories {
    mavenCentral()
}

val include: Configuration by configurations.creating

dependencies {
    // runtime dependency on the old version so we can see it but cant reference it
    runtimeOnly(files("libs/orbital-old.jar"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "samuschair.orbital2.Main"
    }

    from(include.map {
        if (it.isDirectory)
            it
        else
            zipTree(it)
    })
}