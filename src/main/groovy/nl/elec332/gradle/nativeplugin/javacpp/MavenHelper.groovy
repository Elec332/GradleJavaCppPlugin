package nl.elec332.gradle.nativeplugin.javacpp

import org.gradle.api.Project

import java.util.function.Supplier

/**
 * Created by Elec332 on 20-4-2020
 */
class MavenHelper {

    static void fixMavenPom(Project project, Supplier<String> ver) {
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    pom.withXml {
                        Node pomNode = asNode()
                        pomNode.dependencies.'*'.findAll() {
                            it.artifactId.text() == "javacpp" && it.groupId.text() == "org.bytedeco"
                        }.each() {
                            it.version.replaceNode {}
                            it.appendNode("version", ver.get())
                        }
                    }
                }
            }
        }
    }

}
