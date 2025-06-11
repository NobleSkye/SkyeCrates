plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.skyenetwork"
version = "1.0.0"
description = "A Minecraft plugin for customizable crates with loot tables and particle effects"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // Use compileOnly for Gson since it's provided by Paper
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    
    jar {
        archiveBaseName.set("SkyeCrates")
    }
    
    // Use regular jar instead of shadowJar for now
    build {
        dependsOn(jar)
    }
}
