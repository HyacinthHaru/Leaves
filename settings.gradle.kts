pluginManagement {
    repositories {
        mavenLocal() // Leaves - for local leavesweight SNAPSHOT while 2.1.0 is in PR; removed on master
        gradlePluginPortal()
        maven("https://repo.leavesmc.org/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "Leaves"

include("leaves-api", "leaves-server")
