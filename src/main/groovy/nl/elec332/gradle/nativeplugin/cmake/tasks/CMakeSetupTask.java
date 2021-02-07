package nl.elec332.gradle.nativeplugin.cmake.tasks;

import nl.elec332.gradle.nativeplugin.api.cmake.ICMakeSettings;
import nl.elec332.gradle.nativeplugin.cmake.util.CMakeHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

/**
 * Created by Elec332 on 1/24/2021
 */
public class CMakeSetupTask extends DefaultTask {

    @Inject
    public CMakeSetupTask(ICMakeSettings settings) {
        this.settings = settings;
    }

    private final ICMakeSettings settings;
    private FileCollection cmakeLists;
    private FileCollection cmakeFiles;

    @TaskAction
    public void setupCMakeFiles() {
        CMakeHelper.startCMake(getProject(), settings, spec -> {
            spec.setWorkingDir(settings.getBuildDirectory());
            settings.getVariables().get().forEach((nam, arg) -> {
                spec.args("-D" + nam + "=" + arg);
            });
            if (settings.getBuildSharedLibs().isPresent()) {
                spec.args("-DBUILD_SHARED_LIBS=" + (settings.getBuildSharedLibs().get() ? "ON" : "OFF"));
            }
            if (settings.getBuildStaticLibs().isPresent()) {
                spec.args("-DBUILD_STATIC_LIBS=" + (settings.getBuildStaticLibs().get() ? "ON" : "OFF"));
            }

            spec.args("--no-warn-unused-cli");

            spec.args(settings.getProjectDirectory().getAsFile().get().getAbsolutePath());
        });
    }

    @InputFiles
    public FileCollection getCMakeLists() {
        if (cmakeLists == null) {
            cmakeLists = getProject().fileTree(settings.getProjectDirectory(), it -> it.include("**/CMakeLists.txt"));
        }
        return cmakeLists;
    }

    @OutputFiles
    public FileCollection getCmakeFiles() {
        if (cmakeFiles == null) {
            cmakeFiles = getProject().fileTree(settings.getBuildDirectory(), it -> it.include("**/CMakeFiles/**/*").include("**/Makefile").include("**/*.cmake"));
        }
        return cmakeFiles;
    }

    @InputFiles
    public DirectoryProperty getIncludeFiles() {
        return settings.getIncludeDirectory();
    }

}
