val fuelVersion = "2.3.1"

plugins {
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.serialization") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "me.ihdeveloper"
    version = "0.1-dev"

    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        if (project != rootProject) {
            implementation(kotlin("stdlib"))
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
            implementation("com.github.kittinunf.fuel:fuel:$fuelVersion")
            implementation("com.github.kittinunf.fuel:fuel-kotlinx-serialization:$fuelVersion")
            implementation(rootProject)
        } else {
            compileOnly(kotlin("stdlib"))
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
            compileOnly("com.github.kittinunf.fuel:fuel:$fuelVersion")
            compileOnly("com.github.kittinunf.fuel:fuel-kotlinx-serialization:$fuelVersion")
        }
    }

    tasks {

        compileKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = "1.8"
        }

        shadowJar {
            dependsOn("build")

            if (project == rootProject)
                from("LICENSE")

            doLast {
                copy {
                    from("build/libs")
                    into("build")
                }
            }
        }
    }
}
