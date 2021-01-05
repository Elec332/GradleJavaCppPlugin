package nl.elec332.gradle.nativeplugin.api;

import org.gradle.api.provider.SetProperty;

/**
 * Created by Elec332 on 28-12-2020
 */
public interface INativeProjectDependencyHandler {

    SetProperty<String> getDefaultWindowsIncludes();

    SetProperty<String> getDefaultLinuxIncludes();

    void linuxPkgDependency(String name);

    void windowsLinker(String arg);

    void linuxLinker(String arg);

}
