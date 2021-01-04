package nl.elec332.gradle.nativeplugin.api;

import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.Linkage;

public interface INativeProjectExtension {

    SetProperty<Linkage> getLinkage();

    Property<String> getCppVersion();

    Property<String> getBuildToolsInstallDir();

    void modifyCompiler(Action<? super CppCompile> action);

}
