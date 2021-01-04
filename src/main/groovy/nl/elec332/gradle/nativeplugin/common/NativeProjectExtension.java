package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.nativeplugin.api.INativeProjectExtension;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.Linkage;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;


public class NativeProjectExtension implements INativeProjectExtension {

    @Inject
    public NativeProjectExtension(ObjectFactory objectFactory) {
        linkage = objectFactory.setProperty(Linkage.class);
        cppVersion = objectFactory.property(String.class);
        btInstDir = objectFactory.property(String.class);
        compilerMods = new HashSet<>();
    }

    private final SetProperty<Linkage> linkage;
    private final Property<String> cppVersion;
    private final Property<String> btInstDir;
    private final Set<Action<? super CppCompile>> compilerMods;

    @Override
    public SetProperty<Linkage> getLinkage() {
        return linkage;
    }

    @Override
    public Property<String> getCppVersion() {
        return cppVersion;
    }

    @Override
    public Property<String> getBuildToolsInstallDir() {
        return btInstDir;
    }

    @Override
    public void modifyCompiler(Action<? super CppCompile> action) {
        compilerMods.add(action);
    }

    public Set<Action<? super CppCompile>> getCompilerMods() {
        return compilerMods;
    }

}
