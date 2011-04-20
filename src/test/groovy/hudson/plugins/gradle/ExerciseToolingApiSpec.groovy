package hudson.plugins.gradle

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ResultHandler
import org.gradle.tooling.model.HierarchicalProject
import spock.lang.Specification

class ExerciseToolingApiSpec extends Specification {
    def "should run task compileJava on provided build.gradle"() {
        expect:
        def connector = GradleConnector.newConnector()
        def connection = connector.forProjectDirectory(projectDir()).useInstallation(installationDir()).connect()
        connection.getModel(HierarchicalProject.class)
        BuildLauncher launcher = connection.newBuild()
        def outputStream = new ByteArrayOutputStream()
        launcher.setStandardOutput(outputStream)
        launcher.forTasks("clean").run(new ResultHandler() {
            void onComplete(Object result) {
                // FIXME insert real code here
            }

            void onFailure(GradleConnectionException failure) {
                // FIXME insert real code here
            }
        })

        System.err.println();
        System.err.println(outputStream);
    }

    def projectDir() {
        buildFile().parentFile
    }

    def buildFile() {
        def buildScript = ExerciseToolingApiSpec.class.getResource("integrationtest/build.gradle")
        fileFor(buildScript.file)
    }

    def installationDir() {
        fileFor("/usr/local/Cellar/gradle/1.0-milestone-2/libexec/")
    }

    def fileFor(final String fileName) {
        return new File(fileName)
    }
}