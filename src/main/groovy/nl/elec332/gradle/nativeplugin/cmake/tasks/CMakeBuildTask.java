package nl.elec332.gradle.nativeplugin.cmake.tasks;

import nl.elec332.gradle.nativeplugin.cmake.CMakeHelper;
import nl.elec332.gradle.nativeplugin.cmake.ICMakeSettings;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

/**
 * Created by Elec332 on 1/24/2021
 */
@SuppressWarnings("UnstableApiUsage")
public class CMakeBuildTask extends DefaultTask {

    @Inject
    public CMakeBuildTask(ICMakeSettings settings, Property<String> buildType) {
        this.settings = settings;
        this.buildType = buildType;

        ObjectFactory objects = getProject().getObjects();
        cmakeFiles = objects.fileCollection();
        outputDirectory = objects.directoryProperty();
    }

    private final ICMakeSettings settings;
    private final Property<String> buildType;
    private final ConfigurableFileCollection cmakeFiles;
    private final DirectoryProperty outputDirectory;

    @TaskAction
    public void buildProject() {
        CMakeHelper.startCMake(getProject(), settings, spec -> {
            spec.setWorkingDir(settings.getBuildDirectory());
            spec.args("--build", ".");
            spec.args("--config", buildType.get());
            if (settings.getBuildTarget().isPresent()) {
                spec.args("--target", settings.getBuildTarget().get());
            }
        });
    }

    public void usingSetup(CMakeSetupTask setup) {
        dependsOn(setup);
        cmakeFiles.setFrom(setup.getCmakeFiles());
        outputDirectory.set(settings.getBuildDirectory());
    }

    @InputFiles
    public FileCollection getCMakeFiles() {
        return cmakeFiles;
    }

    @OutputDirectory
    public final DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

}
