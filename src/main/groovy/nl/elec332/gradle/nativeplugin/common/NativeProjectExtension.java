package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.nativeplugin.api.INativeProjectDependencyHandler;
import nl.elec332.gradle.nativeplugin.api.INativeProjectExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.Linkage;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

/**
 * Created by Elec332 on 25-12-2020
 */
public class NativeProjectExtension implements INativeProjectExtension {

    @Inject
    public NativeProjectExtension(ObjectFactory objectFactory, ImmutableAttributesFactory attributesFactory, Project project, File file) {
        linkage = objectFactory.setProperty(Linkage.class);
        cppVersion = objectFactory.property(String.class);
        btInstDir = objectFactory.property(String.class);
        compilerMods = new HashSet<>();
        hIncCheck = new HashMap<>();
        hIncCheckFound = new HashSet<>();
        ghf = objectFactory.property(String.class);
        depHandler = new NativeProjectDependencyHandler(objectFactory, attributesFactory, project);
        generatedHeaders = file;

        linkage.set(Collections.singletonList(Linkage.SHARED));
        btInstDir.set("");
        ghf.set(project.getName().trim().toLowerCase(Locale.ROOT));
        cppVersion.set("c++14");
    }

    private final SetProperty<Linkage> linkage;
    private final Property<String> cppVersion;
    private final Property<String> btInstDir;
    private final Set<Action<? super CppCompile>> compilerMods;
    private final Map<String, String> hIncCheck;
    private final Set<String> hIncCheckFound;
    private final Property<String> ghf;
    private final NativeProjectDependencyHandler depHandler;
    private final File generatedHeaders;

    @Override
    public SetProperty<Linkage> getLinkage() {
        return linkage;
    }

    @Override
    public Property<String> getCppVersion() {
        return cppVersion;
    }

    @Override
    public Property<String> getGeneratedHeaderSubFolder() {
        return ghf;
    }

    @Override
    public Property<String> getBuildToolsInstallDir() {
        return btInstDir;
    }

    @Override
    public void checkIncludes(String header, String name) {
        if (hIncCheckFound.add(header)) {
            hIncCheck.put(header, name);
        }
    }

    @Override
    public void dependencies(Action<? super INativeProjectDependencyHandler> action) {
        action.execute(depHandler);
    }

    @Override
    public void modifyCompiler(Action<? super CppCompile> action) {
        compilerMods.add(action);
    }

    public Set<Action<? super CppCompile>> getCompilerMods() {
        return compilerMods;
    }

    public Map<String, String> getHeaderIncludeCheck() {
        return hIncCheck;
    }

    public Set<String> getHeadersFound() {
        return hIncCheckFound;
    }

    public NativeProjectDependencyHandler getDepHandler() {
        return depHandler;
    }

    public File getGeneratedHeadersDir() {
        return generatedHeaders;
    }

}
