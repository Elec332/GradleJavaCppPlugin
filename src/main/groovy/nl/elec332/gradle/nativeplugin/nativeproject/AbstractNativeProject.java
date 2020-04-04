package nl.elec332.gradle.nativeplugin.nativeproject;

import groovy.transform.VisibilityOptions;
import groovy.transform.options.Visibility;
import org.gradle.internal.impldep.com.google.common.collect.HashMultimap;
import org.gradle.internal.impldep.com.google.common.collect.Multimaps;
import org.gradle.internal.impldep.com.google.common.collect.Multiset;
import org.gradle.internal.impldep.com.google.common.collect.SetMultimap;
import org.gradle.internal.impldep.org.apache.commons.collections.MultiMap;

import java.io.File;
import java.util.*;

/**
 * Created by Elec332 on 4-4-2020
 */
public abstract class AbstractNativeProject {

    public AbstractNativeProject(String name) {
        this.name = getName(name);
        this.localDependencies = new HashSet<>();
        this.projectDependencies = new HashSet<>();
        this.platforms = new HashSet<>();
        this.localLibs = HashMultimap.create();
    }

    public final String name;

    final Collection<String> localDependencies, projectDependencies;
    final Collection<String> platforms;
    final SetMultimap<String, File> localLibs;

    protected String getName(String name) {
        return name;
    }

    public void platforms(String... platforms) {
        this.platforms.addAll(Arrays.asList(platforms));
    }

    public void nativeDependency(String libName) {
        this.localDependencies.add(libName);
    }

    public void projectDependency(String nativeProject) {
        projectDependencies.add(nativeProject);
    }

    @VisibilityOptions(Visibility.PRIVATE)
    public Map<String, Collection<File>> getLocalLibraries() {
        return Collections.unmodifiableMap(localLibs.asMap());
    }

}
