package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.util.GroovyHooks;
import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;
import org.gradle.language.cpp.CppBinary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class LinuxHelper {

    @SuppressWarnings("UnstableApiUsage")
    public static String createStaticMergeTask(Project project, CppBinary binary, File linkFile, String... files) {
        String name = "concat_" + binary.getName() + "_" + binary.getBaseName().get();
        project.getTasks().create(name, Exec.class, task -> {
            String file = linkFile.getAbsolutePath();
            String movedFile = file + ".move";
            task.commandLine("mv", file, movedFile);
            task.doLast(t -> {
                project.exec(spec -> {
                    spec.commandLine("ar", "cqT", file, movedFile);
                    spec.args((Object[]) files);
                });
                ByteArrayOutputStream mriFile = new ByteArrayOutputStream();
                GroovyHooks.inject(mriFile, "create " + file + "\n");
                GroovyHooks.inject(mriFile, "addlib " + file + "\n");
                GroovyHooks.inject(mriFile, "save\n");
                GroovyHooks.inject(mriFile, "end\n");
                project.exec(spec -> {
                    spec.setStandardInput(new ByteArrayInputStream(mriFile.toByteArray()));
                    spec.commandLine("ar", "-M");
                });
                if (!project.file(movedFile).delete()) {
                    System.out.println(" Failed to delete: " + movedFile);
                }
            });
        });
        return name;
    }

}
