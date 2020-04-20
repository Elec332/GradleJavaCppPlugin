package nl.elec332.gradle.nativeplugin.javacpp;

import nl.elec332.gradle.nativeplugin.NativePlugin;
import nl.elec332.gradle.nativeplugin.nativeproject.NativeSettings;
import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer;
import org.gradle.api.plugins.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nl.elec332.gradle.nativeplugin.javacpp.MavenHelper.fixMavenPom;

/**
 * Created by Elec332 on 3-4-2020
 */
@NonNullApi
public class JavaCPPPlugin implements Plugin<Project> {

    public static final String COPY_JNI_TASK = "copyJNI";
    public static final String GENERATE_CPP_TASK = "generateCPP";
    public static final String NATIVE_INTERFACE_EXTENSION = "nativeInterfaces";
    public static final String JAVACPP_CONFIGURATION = "javacpp";

    public static final String DEFAULT_JCPP_VERSION = "defaultJavaCPPVersion";

    private static final String JCPP_VERSION = "1.5.2";

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void apply(Project project) {
        project.getPluginManager().apply(NativePlugin.class);
        project.getPluginManager().apply(JavaPlugin.class);

        JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        JavaVersion ver = JavaVersion.VERSION_1_8;
        javaConvention.setSourceCompatibility(ver);
        javaConvention.setTargetCompatibility(ver);

        NativeSettings nativeSettings = Objects.requireNonNull(project.getExtensions().findByType(NativeSettings.class));

        ExtraPropertiesExtension properties = (ExtraPropertiesExtension) project.getExtensions().getByName(ExtraPropertiesExtension.EXTENSION_NAME);
        properties.set(DEFAULT_JCPP_VERSION, JCPP_VERSION);
        project.getRepositories().mavenCentral();

        Configuration jcpp = createJavaCPPConfiguration(project, properties);

        NamedDomainObjectContainer<JNIProject> jniProjects = project.container(JNIProject.class, JNIProject::new);
        project.getExtensions().add(NATIVE_INTERFACE_EXTENSION, jniProjects);

        GenerateCPPTask generateCPPTask = project.getTasks().create(GENERATE_CPP_TASK, GenerateCPPTask.class, jcpp, (Supplier<Collection<JNIProject>>) () -> jniProjects, nativeSettings);
        generateCPPTask.dependsOn(JavaPluginHelper.getJavaCompileTask(project));
        ProjectHelper.getTaskByName(project, NativePlugin.COMPILE_C_START_TASK).dependsOn(generateCPPTask);

        CopyLibrariesTask copyFiles = project.getTasks().create(COPY_JNI_TASK, CopyLibrariesTask.class, (Supplier<Collection<JNIProject>>) () -> jniProjects);
        copyFiles.dependsOn(NativePlugin.COMPILE_C_DONE_TASK);

        JavaPluginHelper.getClassesTask(project).dependsOn(copyFiles);
    }

    @SuppressWarnings("UnstableApiUsage")
    private Configuration createJavaCPPConfiguration(Project project, ExtraPropertiesExtension deps) {
        final Configuration ret = project.getConfigurations().create(JAVACPP_CONFIGURATION);
        Configuration compile = ProjectHelper.getCompileConfiguration(project);
        compile.extendsFrom(ret);

        //Make sure this is the last callback
        //Cannot be run when taskgraph is ready, as defaultDependencies doesn't get called with actions like "clean"
        ProjectHelper.beforeTaskGraphDone(project, () -> ret.withDependencies(a -> {
            if (ret.getAllDependencies().isEmpty()) {
                throw new IllegalStateException("JavaCPP not configured!");
            }
        }));

        project.getPlugins().withType(MavenPlugin.class, plugin -> {
            Object conv = project.getConvention().getPlugins().get("maven");
            if (conv instanceof MavenPluginConvention) {
                ((MavenPluginConvention) conv).getConf2ScopeMappings().addMapping(MavenPlugin.COMPILE_PRIORITY, ret, Conf2ScopeMappingContainer.COMPILE);
                Predicate<ResolvedDependency> isCpp = rd -> rd.getModule().getId().getName().equals("javacpp") && rd.getModule().getId().getGroup().equals("org.bytedeco");
                fixMavenPom(project, () -> {
                    List<ResolvedDependency> t = new ArrayList<>();
                    compile.getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(rd -> {
                        if (!isCpp.test(rd) || !rd.getModuleVersion().equals(JCPP_VERSION)) {
                            addAll(rd, isCpp, t::add);
                        }
                    });
                    return t.isEmpty() ? JCPP_VERSION : t.get(0).getModule().getId().getVersion();
                });
            }
        });

        project.afterEvaluate(p -> {
            ret.defaultDependencies(dependencies -> {
                DomainObjectSet<Dependency> dep = compile.getAllDependencies().matching(element ->
                        element.getGroup() != null && element.getGroup().equals("org.bytedeco") && element.getName().equals("javacpp")
                );

                if (!dep.isEmpty()) {
                    dependencies.addAll(dep);
                } else {
                    dependencies.add(project.getDependencies().create("org.bytedeco:javacpp:" + deps.get(DEFAULT_JCPP_VERSION)));
                }
            });
        });
        return ret;
    }

    private void addAll(ResolvedDependency dep, Predicate<ResolvedDependency> filter, Consumer<ResolvedDependency> consumer) {
        if (filter.test(dep)) {
            consumer.accept(dep);
        }
        for (ResolvedDependency deepDep : dep.getChildren()) {
            addAll(deepDep, filter, consumer);
        }
    }

    @Nonnull
    public static File getGeneratedCppFolder(Project project) {
        return new File(ProjectHelper.getBuildFolder(project), "generated" + File.separator + "cpp");
    }

}
