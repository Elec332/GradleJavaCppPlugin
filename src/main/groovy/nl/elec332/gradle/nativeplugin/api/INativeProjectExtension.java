package nl.elec332.gradle.nativeplugin.api;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.Linkage;

/**
 * Created by Elec332 on 28-12-2020
 */
public interface INativeProjectExtension {

    SetProperty<Linkage> getLinkage();

    Property<String> getCppVersion();

    Property<String> getGeneratedHeaderSubFolder();

    Property<String> getBuildToolsInstallDir();

    void checkIncludes(String header, String name);

    void dependencies(Action<? super INativeProjectDependencyHandler> action);

    void modifyCompiler(Action<? super CppCompile> action);

}
