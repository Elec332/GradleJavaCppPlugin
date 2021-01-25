package nl.elec332.gradle.nativeplugin.cmake;

import nl.elec332.gradle.nativeplugin.common.Constants;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;

/**
 * Created by Elec332 on 1/23/2021
 */
@SuppressWarnings("UnstableApiUsage")
class CMakeSettings implements ICMakeSettings {

    @Inject
    public CMakeSettings(Project project, File projDir, String name, Provider<Directory> buildDir) {
        ObjectFactory objectFactory = project.getObjects();
        this.name = name;

        this.cmakeExecutable = objectFactory.property(String.class);
        this.buildTarget = objectFactory.property(String.class);
        this.buildConfigR = objectFactory.property(String.class);
        this.buildConfigD = objectFactory.property(String.class);
        this.projectDirectory = objectFactory.directoryProperty();
        this.buildDir = objectFactory.directoryProperty();
        this.includeDir = objectFactory.directoryProperty();
        this.varProps = objectFactory.mapProperty(String.class, String.class);
        this.linker = objectFactory.fileCollection();
        this.dynamic = objectFactory.fileCollection();
        this.linkerDebug = objectFactory.fileCollection();
        this.dynamicDebug = objectFactory.fileCollection();
        this.buildSharedLibs = objectFactory.property(Boolean.class);
        this.buildStaticLibs = objectFactory.property(Boolean.class);
        this.linkerInclude = objectFactory.setProperty(String.class);
        this.runtimeInclude = objectFactory.setProperty(String.class);

        if (projDir != Constants.NULL_FILE) {
            this.projectDirectory.set(projDir);
        }
        this.includeDir.set(projectDirectory.dir(Constants.DEFAULT_INCLUDE_FOLDER));
        this.buildDir.set(buildDir);
        this.buildConfigR.set(Constants.CMAKE_RELEASE_CONFIG);
        this.buildConfigD.set(Constants.CMAKE_DEBUG_CONFIG);

    }

    private final String name;
    private final Property<String> cmakeExecutable;
    private final Property<String> buildTarget;
    private final Property<String> buildConfigR;
    private final Property<String> buildConfigD;
    private final DirectoryProperty projectDirectory;
    private final DirectoryProperty buildDir;
    private final DirectoryProperty includeDir;
    private final MapProperty<String, String> varProps;
    private final ConfigurableFileCollection linker;
    private final ConfigurableFileCollection dynamic;
    private final ConfigurableFileCollection linkerDebug;
    private final ConfigurableFileCollection dynamicDebug;
    private final SetProperty<String> linkerInclude;
    private final SetProperty<String> runtimeInclude;
    private final Property<Boolean> buildSharedLibs;
    private final Property<Boolean> buildStaticLibs;

    @Override
    public Property<String> getExecutable() {
        return this.cmakeExecutable;
    }

    @Override
    public Property<String> getBuildTarget() {
        return this.buildTarget;
    }

    @Override
    public Property<String> getReleaseBuildType() {
        return this.buildConfigR;
    }

    @Override
    public Property<String> getDebugBuildType() {
        return this.buildConfigD;
    }

    @Override
    public DirectoryProperty getProjectDirectory() {
        return this.projectDirectory;
    }

    @Override
    public DirectoryProperty getBuildDirectory() {
        return this.buildDir;
    }

    @Override
    public DirectoryProperty getIncludeDirectory() {
        return this.includeDir;
    }

    @Override
    public MapProperty<String, String> getVariables() {
        return this.varProps;
    }

    @Override
    public SetProperty<String> getLinkerBinaries() {
        return this.linkerInclude;
    }

    @Override
    public SetProperty<String> getRuntimeBinaries() {
        return this.runtimeInclude;
    }

    @Override
    public ConfigurableFileCollection getReleaseLinkerBinaries() {
        return this.linker;
    }

    @Override
    public ConfigurableFileCollection getReleaseRuntimeBinaries() {
        return this.dynamic;
    }

    @Override
    public ConfigurableFileCollection getDebugLinkerBinaries() {
        return this.linkerDebug;
    }

    @Override
    public ConfigurableFileCollection getDebugRuntimeBinaries() {
        return this.dynamicDebug;
    }

    @Override
    public void releaseLinkerBinaries(Action<? super ConfigurableFileCollection> action) {
        action.execute(this.linker);
    }

    @Override
    public void releaseRuntimeBinaries(Action<? super ConfigurableFileCollection> action) {
        action.execute(this.dynamic);
    }

    @Override
    public void debugLinkerBinaries(Action<? super ConfigurableFileCollection> action) {
        action.execute(this.linkerDebug);
    }

    @Override
    public void debugRuntimeBinaries(Action<? super ConfigurableFileCollection> action) {
        action.execute(this.dynamicDebug);
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Property<Boolean> getBuildSharedLibs() {
        return buildSharedLibs;
    }

    @Override
    public Property<Boolean> getBuildStaticLibs() {
        return buildStaticLibs;
    }

}
