package nl.elec332.gradle.nativeplugin.nativeproject;

import groovy.transform.VisibilityOptions;
import groovy.transform.options.Visibility;
import nl.elec332.gradle.util.InstalledDependency;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

/**
 * Created by Elec332 on 4-4-2020
 */
public abstract class AbstractNativeProject {

    public AbstractNativeProject(String name) {
        this.name = getName(name);
        this.localDependencies = new HashSet<>();
        this.projectDependencies = new HashSet<>();
        this.platforms = new HashSet<>();
        this.localLibs = new HashMap<>();
        this.installedDependencies = new HashSet<>();
    }

    public final String name;

    final Collection<String> localDependencies, projectDependencies;
    final Collection<InstalledDependency> installedDependencies;
    final Collection<String> platforms;
    private final Map<String, Set<File>> localLibs;

    protected String getName(String name) {
        return name;
    }

    public void platforms(String... platforms) {
        this.platforms.addAll(Arrays.asList(platforms));
    }

    public void nativeDependency(String libName) {
        this.localDependencies.add(libName);
    }

    public void installedLibDependency(String installFolder, String lib, Predicate<String> libPredicate, String include) {
        installedDependency(null, null, installFolder + File.separator + lib, libPredicate, installFolder + File.separator + include);
    }

    public void installedDependency(String installFolder, String bin, String lib, String include) {
        installedDependency(installFolder, bin, null, lib, null, include);
    }

    public void installedDependency(String installFolder, String bin, Predicate<String> binPredicate, String lib, Predicate<String> libPredicate, String include) {
        installedDependency(installFolder + File.separator + bin, binPredicate, installFolder + File.separator + lib, libPredicate, installFolder + File.separator + include);
    }

    public void installedDependency(String bin, Predicate<String> binPredicate, String lib, Predicate<String> libPredicate, String include) {
        this.installedDependencies.add(new InstalledDependency(bin, binPredicate, lib, libPredicate, include));
    }

    public void projectDependency(String nativeProject) {
        projectDependencies.add(nativeProject);
    }

    public void importLibs(AbstractNativeProject project) {
        project.localLibs.forEach((s, libs) -> localLibs.computeIfAbsent(s, sh -> new HashSet<>()).addAll(libs));
    }

    @VisibilityOptions(Visibility.PRIVATE)
    public void putLib(String type, File location) {
        localLibs.computeIfAbsent(type, s -> new HashSet<>()).add(location);
    }

    @VisibilityOptions(Visibility.PRIVATE)
    public Map<String, Collection<File>> getLocalLibraries() {
        return Collections.unmodifiableMap(localLibs);
    }

}
