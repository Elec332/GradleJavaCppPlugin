package nl.elec332.gradle.util;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Created by Elec332 on 31-3-2020
 */
public class JavaPluginHelper {

    @Nonnull
    public static Task getClassesTask(Project project) {
        return Objects.requireNonNull(ProjectHelper.getTaskByName(project, JavaPlugin.CLASSES_TASK_NAME));
    }

    @Nonnull
    public static JavaCompile getJavaCompileTask(Project project) {
        return Objects.requireNonNull((JavaCompile) ProjectHelper.getTaskByName(project, JavaPlugin.COMPILE_JAVA_TASK_NAME));
    }

}
