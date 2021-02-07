package nl.elec332.gradle.nativeplugin.cppproject.extensions;

import nl.elec332.gradle.nativeplugin.api.cppproject.INativeProjectDependencyHandler;
import nl.elec332.gradle.nativeplugin.api.cppproject.INativeProjectExtension;
import nl.elec332.gradle.nativeplugin.base.CppUtilsPlugin;
import nl.elec332.gradle.nativeplugin.cppproject.CppTestPlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
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
    public NativeProjectExtension(ObjectFactory objectFactory, Project project, File file) {
        this.project = project;

        linkage = objectFactory.setProperty(Linkage.class);
        cppVersion = objectFactory.property(String.class);
        btInstDir = objectFactory.property(String.class);
        hIncCheck = new HashMap<>();
        hIncCheckFound = new HashSet<>();
        ghf = objectFactory.property(String.class);
        depHandler = new NativeProjectDependencyHandler(objectFactory, project);
        generatedHeaders = objectFactory.directoryProperty().fileValue(file);
        testArguments = objectFactory.setProperty(String.class);
        staticRuntime = objectFactory.property(Boolean.class);
        minimizeSize = objectFactory.property(Boolean.class);
        generateExportHeader = objectFactory.property(Boolean.class);
        staticTestRuntime = objectFactory.property(Boolean.class);

        linkage.set(Collections.singletonList(Linkage.SHARED));
        btInstDir.set("");
        ghf.set(project.getName().trim().toLowerCase(Locale.ROOT));
        cppVersion.set("c++14");
        staticRuntime.set(false);
        minimizeSize.set(false);
        generateExportHeader.set(false);
        staticTestRuntime.set(true);
    }

    private final Project project;
    private final SetProperty<Linkage> linkage;
    private final Property<String> cppVersion;
    private final Property<String> btInstDir;
    private final Map<String, String> hIncCheck;
    private final Set<String> hIncCheckFound;
    private final Property<String> ghf;
    private final NativeProjectDependencyHandler depHandler;
    private final DirectoryProperty generatedHeaders;
    private final SetProperty<String> testArguments;
    private final Property<Boolean> staticRuntime;
    private final Property<Boolean> minimizeSize;
    private final Property<Boolean> generateExportHeader;
    private final Property<Boolean> staticTestRuntime;

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
        CppUtilsPlugin.getBasePlugin(project).modifyCompiler(action);
    }

    @Override
    public SetProperty<String> getTestArguments() {
        if (!project.getPlugins().hasPlugin(CppTestPlugin.class)) {
            throw new RuntimeException("TestPlugin not present!", new NoSuchMethodException());
        }
        return this.testArguments;
    }

    @Override
    public Property<Boolean> getGenerateExportHeader() {
        return this.generateExportHeader;
    }

    @Override
    public Property<Boolean> getStaticRuntime() {
        return this.staticRuntime;
    }

    @Override
    public Property<Boolean> getMinimizeSize() {
        return this.minimizeSize;
    }

    @Override
    public Property<Boolean> getStaticTestRuntime() {
        return this.staticTestRuntime;
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

    public DirectoryProperty getGeneratedHeadersDir() {
        return generatedHeaders;
    }

}
