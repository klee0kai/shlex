package maven_publish

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get


fun PublishingExtension.shlexToMaven(project: Project) {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            when {
                "java" in project.components.names -> {
                    //publish simple java lib
                    from(project.components["java"])
                }
                "release" in project.components.names -> {
                    //publish android lib
                    from(project.components["release"])
                }
            }

            pom {
                name.set("Shlex")
                description.set("Simple lexical analysis")
                url.set("https://github.com/klee0kai/shlex")
                licenses {
                    license {
                        name.set("GNU General Public License, Version 3")
                        url.set("https://github.com/klee0kai/shlex/blob/dev/LICENCE.md")
                    }
                }
                developers {
                    developer {
                        id.set("klee0kai")
                        name.set("Andrey Kuzubov")
                        email.set("klee0kai@gmail.com")
                    }
                }
            }
        }
    }
}