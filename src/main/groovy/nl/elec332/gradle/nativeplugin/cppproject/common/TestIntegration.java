package nl.elec332.gradle.nativeplugin.cppproject.common;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.language.cpp.CppLibrary;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;
import org.gradle.nativeplatform.test.cpp.plugins.CppUnitTestPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Elec332 on 2/9/2021
 */
public class TestIntegration {

    @SuppressWarnings("all")
    public static void fixTestExecutable(Project project) { //TODO: Restore my eyesight
        if (project.getPlugins().hasPlugin(CppUnitTestPlugin.class)) {
            Set<CppCompile> c = new HashSet<>();
            project.getExtensions().getByType(CppLibrary.class).getBinaries().whenElementFinalized(b -> {
                c.add(b.getCompileTask().get());
            });
            project.getExtensions().getByType(CppTestSuite.class).getBinaries().whenElementFinalized(b -> {
                Configuration linkLibraries = (Configuration) project.getExtensions().getByType(CppTestSuite.class).getTestBinary().get().getLinkLibraries();
                Configuration test = project.getConfigurations().create(linkLibraries.getName() + "Extended");
                linkLibraries.extendsFrom(test);
                boolean removed = false;
                for (Dependency d : new ArrayList<>(linkLibraries.getDependencies())) {
                    if (c.containsAll(((SelfResolvingDependency) d).getBuildDependencies().getDependencies(null))) {
                        linkLibraries.getDependencies().remove(d);
                        removed = true;
                    }
                }
                if (removed) {
                    project.getDependencies().add(test.getName(), project.getDependencies().create(project));
                }
            });
        }
    }

}
