package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.nativeplugin.nativeproject.NativeProject;
import nl.elec332.gradle.nativeplugin.nativeproject.NativeProjectManager;
import nl.elec332.gradle.nativeplugin.nativeproject.NativeSettings;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.*;
import org.gradle.language.cpp.plugins.CppPlugin;

/**
 * Created by Elec332 on 30-3-2020
 */
@NonNullApi
public class NativePlugin implements Plugin<Project> {

    public static final String COMPILE_C_START_TASK = "compileCStart";
    public static final String COMPILE_C_DONE_TASK = "compileC";

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion("5.0");
        project.getPluginManager().apply(CppPlugin.class);

        NativeSettings settings = project.getExtensions().create("nativeSettings", NativeSettings.class, project);
        NamedDomainObjectContainer<NativeProject> projects = project.container(NativeProject.class, name -> new NativeProject(name, project));
        project.getExtensions().add("nativeProjects", projects);


        Task compileCPre = project.getTasks().create(COMPILE_C_START_TASK, DefaultTask.class);

        Task compileC = project.getTasks().create(COMPILE_C_DONE_TASK, DefaultTask.class);
        compileC.dependsOn(compileCPre)
                .mustRunAfter(compileCPre)
                .doLast(a -> System.out.println("Compiled native code"));

        project.afterEvaluate(p -> NativeProjectManager.registerNativeProjects(p, settings, projects));

        ProjectHelper.getBuildTask(project).dependsOn(compileC);
    }

}
