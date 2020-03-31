package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.language.cpp.plugins.CppPlugin;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Created by Elec332 on 30-3-2020
 */
@NonNullApi
public class NativePlugin implements Plugin<Project> {

    public static final String GENERATE_CPP_TASK = "generateCPP";
    public static final String COMPILE_C_PRE_TASK = "compileCPre";
    public static final String COMPILE_C_TASK = "compileC";
    public static final String GENERATE_JNI_TASK = "generateJNI";
    public static final String NATIVE_DEPENDENCIES_EXTENSION = "nativeDependencies";
    public static final String JAVACPP_CONFIGURATION = "javacpp";

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void apply(Project project) {
        project.getPluginManager().apply(CppPlugin.class);
        project.getPluginManager().apply(JavaPlugin.class);

        NativeDependencies dependencies = expandDependencies(project);
        Configuration jcpp = createJavaCPPConfiguration(project, dependencies);

        GenerateCPPTask generateCPPTask = addCPPTask(project, jcpp);

        Task compileCPre = project.getTasks().create(COMPILE_C_PRE_TASK, DefaultTask.class);
        compileCPre.dependsOn(generateCPPTask);

        Task compileC = project.getTasks().create(COMPILE_C_TASK, DefaultTask.class);
        compileC.dependsOn(generateCPPTask).dependsOn(compileCPre).doLast(a -> System.out.println("Compiled native code"));

        GenerateJNITask generateJNI = project.getTasks().create(GENERATE_JNI_TASK, GenerateJNITask.class);
        generateJNI.dependsOn(compileC);

        JavaPluginHelper.getClassesTask(project).dependsOn(generateJNI);
    }

    private Configuration createJavaCPPConfiguration(Project project, NativeDependencies deps) {
        final Configuration ret = project.getConfigurations().create(JAVACPP_CONFIGURATION);
        ProjectHelper.getCompileConfiguration(project).extendsFrom(ret);
        project.getGradle().getTaskGraph().whenReady(graph -> {
            if (ret.getAllDependencies().isEmpty()) {
                throw new IllegalStateException("JavaCPP not configured!");
            }
        });
        ret.defaultDependencies(dependencies -> dependencies.add(project.getDependencies().create("org.bytedeco:javacpp:" + deps.defaultJavaCPPVersion)));
        return ret;
    }

    private NativeDependencies expandDependencies(Project project) {
        return project.getExtensions().create(NATIVE_DEPENDENCIES_EXTENSION, NativeDependencies.class, project);
    }

    @SuppressWarnings("UnstableApiUsage")
    private GenerateCPPTask addCPPTask(Project project, Configuration jcpp) {
        GenerateCPPTask ret = project.getTasks().create(GENERATE_CPP_TASK, GenerateCPPTask.class, jcpp);
        ret.dependsOn(JavaPluginHelper.getJavaCompileTask(project));
        return ret;
    }

    @Nonnull
    public static NativeDependencies getNativeDependencies(Project project) {
        return Objects.requireNonNull((NativeDependencies) project.getExtensions().getByName(NATIVE_DEPENDENCIES_EXTENSION));
    }

}
