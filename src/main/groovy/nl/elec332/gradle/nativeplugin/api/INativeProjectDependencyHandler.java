package nl.elec332.gradle.nativeplugin.api;

import groovy.lang.Closure;
import nl.elec332.gradle.nativeplugin.cmake.ICMakeSettings;
import org.gradle.api.Action;
import org.gradle.api.provider.SetProperty;
import org.gradle.util.ConfigureUtil;

/**
 * Created by Elec332 on 28-12-2020
 */
public interface INativeProjectDependencyHandler {

    SetProperty<String> getDefaultWindowsIncludes();

    SetProperty<String> getDefaultLinuxIncludes();

    void linuxPkgDependency(String name);

    void windowsLinker(String arg);

    void linuxLinker(String arg);

    default void cmakeDependency(String name, Closure<?> closure) {
        cmakeDependency(name, settings -> ConfigureUtil.configure(closure, settings));
    }

    void cmakeDependency(String name, Action<? super ICMakeSettings> modifier);

}
