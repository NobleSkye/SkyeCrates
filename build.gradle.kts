plugins {
    kotlin("jvm") version "1.9.22"
    id("io.papermc.paperweight.userdev") version "1.5.11"
    id("xyz.jpenilla.run-paper") version "2.2.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.skyenetwork"
version = "1.0.0"
description = "A Minecraft plugin for customizable crates with loot tables and particle effects"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.kyori:adventure-api:4.15.0")
    implementation("net.kyori:adventure-text-minimessage:4.15.0")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    
    assemble {
        dependsOn(reobfJar)
    }
    
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    
    shadowJar {
        minimize()
    }
    
    runServer {
        minecraftVersion("1.21.4")
    }
}
