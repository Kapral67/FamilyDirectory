import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.provider.ListProperty

abstract class CheckEnvironmentTask extends DefaultTask {

    @Input
    abstract ListProperty<String> getRequiredEnvironmentVariables()

    @TaskAction
    void checkEnvironment() {
        def missingEnvVars = getRequiredEnvironmentVariables().get().findAll { System.getenv(it) == null }

        if (!missingEnvVars.isEmpty()) {
            throw new GradleException("Missing environment variables: ${missingEnvVars.toString()}")
        }
    }
}
