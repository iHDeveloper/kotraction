plugins {
    kotlin("jvm") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.github.johnrengelman.shadow")

    group = "me.ihdeveloper"
    version = "0.1-dev"

    repositories {
        mavenCentral()
    }

    dependencies {
        if (project != rootProject) {
            implementation(kotlin("stdlib"))
            implementation(rootProject)
        } else {
            compileOnly(kotlin("stdlib"))
        }
    }

    tasks {
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
