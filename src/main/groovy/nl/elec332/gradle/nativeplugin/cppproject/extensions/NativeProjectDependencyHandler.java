package nl.elec332.gradle.nativeplugin.cppproject.extensions;

import nl.elec332.gradle.nativeplugin.api.cppproject.INativeProjectDependencyHandler;
import nl.elec332.gradle.nativeplugin.base.CppUtilsPlugin;
import nl.elec332.gradle.nativeplugin.cmake.util.CMakeHelper;
import nl.elec332.gradle.nativeplugin.api.cmake.ICMakeSettings;
import nl.elec332.gradle.nativeplugin.cppproject.common.AbstractCppPlugin;
import nl.elec332.gradle.nativeplugin.util.Constants;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.internal.metaobject.MethodAccess;
import org.gradle.internal.metaobject.MethodMixIn;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 5-1-2021
 */
public class NativeProjectDependencyHandler implements INativeProjectDependencyHandler, MethodMixIn {

    NativeProjectDependencyHandler(ObjectFactory objectFactory, Project project) {
        this.project = project;

        this.configurator = new DynamicDependencyMethods(allowedConfigs::contains, project);
        this.winInc = objectFactory.setProperty(String.class);
        this.linInc = objectFactory.setProperty(String.class);
        this.pkgs = new HashSet<>();
        this.winLink = new HashSet<>();
        this.linLink = new HashSet<>();

        this.winInc.set(Constants.WINDOWS_INCLUDES);
        this.linInc.set(Constants.LINUX_INCLUDES);
    }

    private static final Collection<String> allowedConfigs = Arrays.asList("implementation", "testImplementation",
            AbstractCppPlugin.STATIC_LINKER,
            CppUtilsPlugin.RUNTIME, CppUtilsPlugin.RUNTIME_RELEASE, CppUtilsPlugin.RUNTIME_DEBUG,
            CppUtilsPlugin.LINKER, CppUtilsPlugin.LINKER_RELEASE, CppUtilsPlugin.LINKER_DEBUG,
            CppUtilsPlugin.HEADERS, CppUtilsPlugin.WINDOWS_HEADERS);

    private final Project project;
    private final MethodAccess configurator;
    private final SetProperty<String> winInc;
    private final SetProperty<String> linInc;
    private final Set<String> pkgs, winLink, linLink;

    @Override
    public MethodAccess getAdditionalMethods() {
        return this.configurator;
    }

    @Override
    public SetProperty<String> getDefaultWindowsIncludes() {
        return winInc;
    }

    @Override
    public SetProperty<String> getDefaultLinuxIncludes() {
        return linInc;
    }

    @Override
    public void linuxPkgDependency(String name) {
        pkgs.add(name);
    }

    @Override
    public void windowsLinker(String arg) {
        winLink.add(arg);
    }

    @Override
    public void linuxLinker(String arg) {
        linLink.add(arg);
    }

    @Override
    public void cmakeDependency(String name, Action<? super ICMakeSettings> modifier) {
        CMakeHelper.registerCMakeDependency(project, name, modifier);
    }

    public Set<String> getLinuxPkgDeps() {
        return pkgs;
    }

    public Set<String> getWinLink() {
        return winLink;
    }

    public Set<String> getLinLink() {
        return linLink;
    }

}
