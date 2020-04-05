package nl.elec332.gradle.nativeplugin.nativeproject;

import groovy.transform.VisibilityOptions;
import groovy.transform.options.Visibility;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 3-4-2020
 */
public class NativeSettings {

    @SuppressWarnings("UnstableApiUsage")
    public NativeSettings(Project project) {
        this.libRootFolder = ProjectHelper.getProjectDirPath(project) + File.separator + "clibs";
        this.defaultSource = project.getObjects().sourceDirectorySet("defaultSources", "Default Sources");
        this.defaultHeaders = project.getObjects().sourceDirectorySet("defaultHeaders", "Default Headers");
        this.localDependencies = new ArrayList<>();
        this.customProjects = new HashSet<>();
        this.applyDefaultPlatforms = true;
    }

    public final SourceDirectorySet defaultSource, defaultHeaders;
    private final Collection<String> localDependencies;
    final Collection<Consumer<BiConsumer<String, Action<? super NativeProject>>>> customProjects;

    @Inject
    public String libRootFolder;

    @Inject
    public boolean applyDefaultPlatforms;

    public void nativeDependency(String libName) {
        localDependencies.add(libName);
    }

    @VisibilityOptions(Visibility.PRIVATE)
    public void addCustomProjects(Consumer<BiConsumer<String, Action<? super NativeProject>>> consumer) {
        this.customProjects.add(consumer);
    }

    @VisibilityOptions(Visibility.PRIVATE)
    Collection<String> getProjectDependencies(NativeProject nativeProject) {
        return nativeProject.projectDependencies;
    }

    @VisibilityOptions(Visibility.PRIVATE)
    Collection<String> getLocalDependencies(NativeProject nativeProject) {
        Set<String> ret = new HashSet<>();
        ret.addAll(localDependencies);
        ret.addAll(nativeProject.localDependencies);
        return ret;
    }

    @VisibilityOptions(Visibility.PRIVATE)
    void getSource(SourceDirectorySet sources, NativeProject nativeProject) {
        sources.source(nativeProject.source);
        sources.source(defaultSource);
        if (nativeProject.includeDefaultCCode) {
            sources.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(nativeProject.project) + "/c/common/" + nativeProject.name);
        }
        if (nativeProject.includeDefaultCPPCode) {
            sources.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(nativeProject.project) + "/cpp/common/" + nativeProject.name);
        }
    }

    @VisibilityOptions(Visibility.PRIVATE)
    void getHeaders(SourceDirectorySet headers, NativeProject nativeProject) {
        headers.source(nativeProject.headers);
        headers.source(defaultHeaders);
        if (nativeProject.includeDefaultCHeaders) {
            headers.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(nativeProject.project) + "/c/common/headers");
        }
        if (nativeProject.includeDefaultCPPHeaders) {
            headers.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(nativeProject.project) + "/cpp/common/headers");
        }
        for (String s : getLocalDependencies(nativeProject)) {
            nativeProject.headers.srcDir(libRootFolder + "/" + s + "/include");
        }
    }

}
