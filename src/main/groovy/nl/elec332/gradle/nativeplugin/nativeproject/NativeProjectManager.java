package nl.elec332.gradle.nativeplugin.nativeproject;

import nl.elec332.gradle.nativeplugin.DefaultPlatforms;
import nl.elec332.gradle.nativeplugin.NativePlugin;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.*;
import java.util.function.Function;

/**
 * Created by Elec332 on 3-4-2020
 */
public class NativeProjectManager {

    public static void registerNativeProjects(Project project, NativeSettings settings, NamedDomainObjectContainer<NativeProject> projects) {
        if (settings.applyDefaultPlatforms) {
            DefaultPlatforms.registerDefaultPlatforms(project);
        }
        settings.customProjects.forEach(c -> c.accept(projects::register));
        NativeProject[] sortedProjects = sortProjects(projects);
        Task previousStep = project.getTasks().getByName(NativePlugin.COMPILE_C_START_TASK);
        for (NativeProject proj : sortedProjects) {
            previousStep = registerNativeProjectTask(settings, proj, previousStep, projects::getByName);
        }
        project.getTasks().getByName(NativePlugin.COMPILE_C_DONE_TASK).dependsOn(previousStep);
        ProjectHelper.afterNativeModelExamined(project, () -> {
            for (NativeProject nativeProject : sortedProjects) {
                for (String s : settings.getProjectDependencies(nativeProject)) {
                    nativeProject.localLibs.putAll(Objects.requireNonNull(projects.getByName(s)).localLibs);
                    nativeProject.localLinks.addAll(Objects.requireNonNull(projects.getByName(s)).localLinks);
                }
                nativeProject.cfgCallback.forEach(Runnable::run);
            }
        });
    }

    private static Task registerNativeProjectTask(NativeSettings settings, NativeProject nativeProject, Task prev, Function<String, NativeProject> allProjects) {
        final Task next = nativeProject.project.getTasks().create("compileDone-" + nativeProject.name);
        next.doLast(t -> System.out.println("Finished compiling " + nativeProject.name));

        nativeProject.configureSpec(spec -> {
            String name = spec.getName();
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            String lName = nativeProject.name;
            lName = lName.substring(0, 1).toUpperCase() + lName.substring(1);
            String fullName = "compile" + lName + name + lName + "Cpp";
            String doneName = nativeProject.name + name;
            prev.finalizedBy(fullName);

            next.dependsOn(prev);
            next.mustRunAfter(doneName);
        });

        NativeProjectRegister.registerNativeProject(nativeProject.project, settings, nativeProject, allProjects);

        return next;
    }

    private static NativeProject[] sortProjects(NamedDomainObjectContainer<NativeProject> projects) {
        List<NativeProject> allProjects = new ArrayList<>(projects);
        NativeProject[] sortedProjects = new NativeProject[allProjects.size()];
        int counter = 0, tries = 0;
        Iterator<NativeProject> it = allProjects.iterator();
        while (it.hasNext()) {
            NativeProject p = it.next();
            if (p.projectDependencies.size() == 0) {
                sortedProjects[counter] = p;
                counter++;
                it.remove();
            }
        }
        while (!allProjects.isEmpty()) {
            if (tries > sortedProjects.length * 2) {
                throw new RuntimeException("Failed to sort projects");
            }
            it = allProjects.iterator();
            while (it.hasNext()) {
                NativeProject p = it.next();
                for (String s : p.projectDependencies) {
                    if (Arrays.stream(sortedProjects).anyMatch(pr -> pr.name.equals(s))) {
                        sortedProjects[counter] = p;
                        counter++;
                        it.remove();
                    }
                }
            }
            tries++;
        }
        return sortedProjects;
    }

}
