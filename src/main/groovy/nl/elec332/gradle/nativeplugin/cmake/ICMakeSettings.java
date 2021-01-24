package nl.elec332.gradle.nativeplugin.cmake;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * Created by Elec332 on 1/23/2021
 */
@SuppressWarnings("UnstableApiUsage")
public interface ICMakeSettings extends Named {

    Property<String> getExecutable();

    Property<String> getBuildTarget();

    Property<String> getReleaseBuildType();

    Property<String> getDebugBuildType();

    DirectoryProperty getProjectDirectory();

    DirectoryProperty getBuildDirectory();

    DirectoryProperty getIncludeDirectory();

    MapProperty<String, String> getVariables();

    ConfigurableFileCollection getLinkerBinaries();

    ConfigurableFileCollection getRuntimeLibraries();

    void linkerBinaries(Action<? super ConfigurableFileCollection> action);

    void runtimeLibraries(Action<? super ConfigurableFileCollection> action);

    Property<Boolean> getBuildSharedLibs();

    Property<Boolean> getBuildStaticLibs();

}
