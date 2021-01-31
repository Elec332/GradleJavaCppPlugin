package nl.elec332.gradle.nativeplugin.cppproject.common;

import nl.elec332.gradle.util.GroovyHooks;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Exec;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppStaticLibrary;
import org.gradle.nativeplatform.tasks.CreateStaticLibrary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Set;

/**
 * Created by Elec332 on 1/30/2021
 */
@SuppressWarnings("UnstableApiUsage")
public class StaticLibraryHandler {

    static void mergeStaticLibraries(Project project, CppBinary binary) {
        Configuration staticConfig = project.getConfigurations().getAt(AbstractCppPlugin.STATIC_LINKER);
        if (binary instanceof CppStaticLibrary) {
            Set<File> deps = staticConfig.resolve();
            if (binary.getTargetPlatform().getTargetMachine().getOperatingSystemFamily().isWindows()) {
                deps.forEach(file -> ((CreateStaticLibrary) ((CppStaticLibrary) binary).getLinkFileProducer().get()).source(project.files(file)));
            } else {
                ((CppStaticLibrary) binary).getCreateTask().get().finalizedBy(createStaticMergeTaskLinux(project, binary, ((CppStaticLibrary) binary).getLinkFile().get().getAsFile(), deps.stream().map(File::getAbsolutePath).toArray(String[]::new)));
            }
        } else {
            ((Configuration) binary.getLinkLibraries()).extendsFrom(staticConfig);
        }
    }

    private static String createStaticMergeTaskLinux(Project project, CppBinary binary, File linkFile, String... files) {
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
                    System.out.println("Failed to delete: " + movedFile);
                }
            });
        });
        return name;
    }

}
