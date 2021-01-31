package nl.elec332.gradle.nativeplugin.api.cmake;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

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


    SetProperty<String> getLinkerBinaries();

    SetProperty<String> getRuntimeBinaries();

    ConfigurableFileCollection getReleaseLinkerBinaries();

    ConfigurableFileCollection getReleaseRuntimeBinaries();

    ConfigurableFileCollection getDebugLinkerBinaries();

    ConfigurableFileCollection getDebugRuntimeBinaries();

    void releaseLinkerBinaries(Action<? super ConfigurableFileCollection> action);

    void releaseRuntimeBinaries(Action<? super ConfigurableFileCollection> action);

    void debugLinkerBinaries(Action<? super ConfigurableFileCollection> action);

    void debugRuntimeBinaries(Action<? super ConfigurableFileCollection> action);


    Property<Boolean> getBuildSharedLibs();

    Property<Boolean> getBuildStaticLibs();

}
