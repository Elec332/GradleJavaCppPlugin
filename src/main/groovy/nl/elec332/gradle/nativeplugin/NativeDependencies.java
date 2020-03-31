package nl.elec332.gradle.nativeplugin;

import groovy.transform.VisibilityOptions;
import groovy.transform.options.Visibility;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Elec332 on 31-3-2020
 */
public class NativeDependencies {

    public NativeDependencies(Project project) {
        this.clibs = new HashMap<>();
        this.libRootFolder = ProjectHelper.getProjectDirPath(project) + File.separator + "clibs";
    }

    private final Map<String, List<String>> clibs;

    @Inject
    public String defaultJavaCPPVersion = "1.4.3";

    @Inject
    public String libRootFolder;

    @VisibilityOptions(Visibility.PRIVATE)
    List<String> getLibs(String pack) {
        List<String> ret = clibs.get(pack);
        if (ret == null) {
            ret = Collections.emptyList();
        }
        return Collections.unmodifiableList(ret);
    }

    @VisibilityOptions(Visibility.PRIVATE)
    Collection<String> getAllLibs() {
        return Collections.unmodifiableCollection(clibs.values().stream().flatMap(List::stream).collect(Collectors.toList()));
    }

    public void addCLibrary(String name) {
        addCLibrary("", name);
    }

    public void addCLibrary(String pack, String name) {
        pack = pack.replace(".", "/");
        clibs.computeIfAbsent(pack, s -> new ArrayList<>()).add(name);
    }

    public void addInstalledCLibrary(String s1, String s2) {
    }

}
