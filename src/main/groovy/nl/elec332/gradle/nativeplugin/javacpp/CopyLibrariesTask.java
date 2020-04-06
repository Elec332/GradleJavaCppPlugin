package nl.elec332.gradle.nativeplugin.javacpp;

import nl.elec332.gradle.util.FileHelper;
import nl.elec332.gradle.util.JavaPluginHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Created by Elec332 on 3-4-2020
 */
public class CopyLibrariesTask extends DefaultTask {

    @Inject
    public CopyLibrariesTask(Supplier<Collection<JNIProject>> projects) {
        this.projects = projects;
    }

    private final Supplier<Collection<JNIProject>> projects;

    @TaskAction
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void copyFiles() {
        JavaCompile jc = JavaPluginHelper.getJavaCompileTask(getProject());
        File destinationDir = jc.getDestinationDir();
        for (JNIProject project : projects.get()) {
            String rootDest = destinationDir.getAbsolutePath() + File.separator + project.classPackage.replace(".", File.separator);
            project.getLocalLibraries().forEach((s, files) -> {
                File platformDir = new File(rootDest, s);
                FileHelper.cleanFolder(platformDir);
                files.forEach(libLocation -> {
                    File dest = new File(platformDir, libLocation.getName());
                    try {
                        Files.copy(libLocation.toPath(), dest.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            });
        }
    }

}
