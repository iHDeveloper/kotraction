val ktorVersion = "1.5.0"

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                "Main-Class" to "me.ihdeveloper.kotraction.test.Main"
            ))
        }
    }
}
