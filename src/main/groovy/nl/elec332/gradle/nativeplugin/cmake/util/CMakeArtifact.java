package nl.elec332.gradle.nativeplugin.cmake.util;

import nl.elec332.gradle.util.Utils;
import nl.elec332.gradle.util.abstraction.ITaskDependency;
import org.gradle.api.Project;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.tasks.TaskDependency;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Date;

/**
 * Created by Elec332 on 1/25/2021
 */
class CMakeArtifact implements PublishArtifact {

    public CMakeArtifact(Project project, String name, File file, Object... tasks) {
        this.taskDependency = Utils.newTaskDependency(project);
        this.name = name;
        this.file = file;

        this.taskDependency.add(tasks);
    }

    private final ITaskDependency taskDependency;
    private final String name;
    private final File file;

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public String getExtension() {
        return "unknown";
    }

    @Nonnull
    @Override
    public String getType() {
        return "cmake";
    }

    @Nullable
    @Override
    public String getClassifier() {
        return "classifier";
    }

    @Nonnull
    @Override
    public File getFile() {
        return file;
    }

    @Nullable
    @Override
    public Date getDate() {
        return null;
    }

    @Nonnull
    @Override
    public TaskDependency getBuildDependencies() {
        return taskDependency;
    }

}
