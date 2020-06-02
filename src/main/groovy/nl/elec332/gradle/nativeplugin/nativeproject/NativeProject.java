package nl.elec332.gradle.nativeplugin.nativeproject;

import groovy.transform.VisibilityOptions;
import groovy.transform.options.Visibility;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.nativeplatform.NativeBinarySpec;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 3-4-2020
 */
public class NativeProject extends AbstractNativeProject {

    @SuppressWarnings("UnstableApiUsage")
    public NativeProject(String name, Project project) {
        super(name);
        this.project = project;

        this.source = project.getObjects().sourceDirectorySet("sources", "Sources");
        this.source.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(project) + "/c/" + name);
        this.source.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(project) + "/cpp/" + name);

        this.headers = project.getObjects().sourceDirectorySet("headers", "Headers");
        this.headers.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(project) + "/c/" + name + "/headers");
        this.headers.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(project) + "/cpp/" + name + "/headers");

        this.cfgCallback = new HashSet<>();
        this.localLinks = new HashSet<>();
        this.specCallback = new HashSet<>();
    }

    @VisibilityOptions(Visibility.PRIVATE)
    public void importFrom(AbstractNativeProject otherProject) {
        localDependencies.addAll(otherProject.localDependencies);
        installedDependencies.addAll(otherProject.installedDependencies);
        platforms.addAll(otherProject.platforms);
        projectDependencies.addAll(otherProject.projectDependencies);
        onConfigured(() -> otherProject.importLibs(NativeProject.this));
    }

    public final SourceDirectorySet source;
    public final SourceDirectorySet headers;

    final Project project;
    final Collection<File> localLinks;
    final Collection<Runnable> cfgCallback;
    final Collection<Consumer<NativeBinarySpec>> specCallback;

    @Inject
    public boolean includeDefaultCCode = true;

    @Inject
    public boolean includeDefaultCPPCode = true;

    @Inject
    public boolean includeDefaultCHeaders = true;

    @Inject
    public boolean includeDefaultCPPHeaders = true;

    @Inject
    public boolean excludeStaticLibraryCollection = true;

    public void onConfigured(Runnable callback) {
        cfgCallback.add(callback);
    }

    public void configureSpec(Consumer<NativeBinarySpec> callback) {
        specCallback.add(callback);
    }

    @Override
    public String toString() {
        return "NativeProject{" +
                "name='" + name + '\'' +
                ", localDependencies=" + localDependencies +
                ", installedDependencies=" + installedDependencies +
                ", projectDependencies=" + projectDependencies +
                ", platforms=" + platforms +
                '}';
    }

}
