package nl.elec332.gradle.nativeplugin.javacpp;

import nl.elec332.gradle.nativeplugin.api.cppproject.INativeProjectExtension;
import nl.elec332.gradle.nativeplugin.base.CppUtilsPlugin;
import nl.elec332.gradle.nativeplugin.cppproject.CppLibraryPlugin;
import nl.elec332.gradle.nativeplugin.javacpp.tasks.GenerateCppTask;
import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.nativeplugin.util.NativeHelper;
import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.cpp.CppSharedLibrary;
import org.gradle.language.cpp.CppStaticLibrary;
import org.gradle.language.cpp.internal.DefaultCppBinary;
import org.gradle.nativeplatform.Linkage;
import org.gradle.nativeplatform.platform.OperatingSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Elec332 on 2/20/2021
 */
@NonNullApi
public class JavaCppPlugin implements Plugin<Project> {

    public static final String JAVACPP_CONFIGURATION = "javacpp";

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        project.getPluginManager().apply(CppLibraryPlugin.class);
        project.getPluginManager().apply(JavaPlugin.class);

        INativeProjectExtension nativeProjectExtension = Objects.requireNonNull(project.getExtensions().findByType(INativeProjectExtension.class));
        nativeProjectExtension.getLinkage().set(Collections.singleton(Linkage.SHARED));
        nativeProjectExtension.getSingleRuntimeType().set(true);
        nativeProjectExtension.getStaticTestRuntime().set(nativeProjectExtension.getStaticRuntime());
        project.afterEvaluate(p -> {
            if (nativeProjectExtension.getLinkage().get().size() != 1 || nativeProjectExtension.getLinkage().get().iterator().next() != Linkage.SHARED) {
                throw new IllegalStateException("Linkage can only be SHARED!");
            }
            if (!nativeProjectExtension.getSingleRuntimeType().get()) {
                throw new IllegalStateException("Cannot have multiple runtime components!");
            }
            if (nativeProjectExtension.getStaticRuntime().get() != nativeProjectExtension.getStaticTestRuntime().get()) {
                throw new IllegalStateException("Normal & test runtime types must match!!");
            }
        });

        IJavaCppExtension javaCppExtension = project.getExtensions().create(IJavaCppExtension.class, "javaCpp", JavaCppExtension.class);
        project.getRepositories().add(project.getRepositories().mavenCentral());
        Configuration jcpp = createJavaCPPConfiguration(project, javaCppExtension);
        Configuration cppImplementation = project.getConfigurations().create("cppImplementation");

        TaskProvider<GenerateCppTask> generateTask = project.getTasks().register("javaCppGenerate", GenerateCppTask.class, jcpp, javaCppExtension);
        nativeProjectExtension.modifyCompiler(c -> {
            c.dependsOn(generateTask);
            c.source(project.fileTree(getGeneratedCppFolder(project), t -> t.include("**/*.cpp")));
            OperatingSystem os = c.getTargetPlatform().get().getOperatingSystem();

            String javaHome = JavaPluginHelper.getJavaHome();
            if (os.isMacOsX()) {
                c.getIncludes().from(javaHome + "/include");
                c.getIncludes().from(javaHome + "/include/darwin");
            } else if (os.isLinux()) {
                c.getIncludes().from(javaHome + "/include");
                c.getIncludes().from(javaHome + "/include/linux");
            } else if (os.isWindows()) {
                c.getIncludes().from(javaHome + "/include");
                c.getIncludes().from(javaHome + "/include/win32");
            } else if (os.isFreeBSD()) {
                c.getIncludes().from(javaHome + "/include");
                c.getIncludes().from(javaHome + "/include/freebsd");
            }

        });

        CppUtilsPlugin.getBasePlugin(project).modifyBinaries(binary -> {
            String[] stuff = javaCppExtension.getMainClass().get().split("\\.");
            ((Property<String>) binary.getBaseName()).set("jni" + stuff[stuff.length - 1]);
            clearLinks(((DefaultCppBinary) binary).getImplementationDependencies(), project.getConfigurations().getByName("implementation"), cppImplementation);
            if (!NativeHelper.isDebug(binary)) {
                if (binary instanceof CppStaticLibrary) {
                    PlatformHandler.addPlatformJar(project, (CppStaticLibrary) binary, javaCppExtension, ((CppStaticLibrary) binary).getCreateTask().get());
                } else if (binary instanceof CppSharedLibrary) {
                    PlatformHandler.addPlatformJar(project, (CppSharedLibrary) binary, javaCppExtension, ((CppSharedLibrary) binary).getLinkTask().get());
                }
            }
        });

    }

    private static void clearLinks(@Nullable Configuration tested, @Nullable Configuration remove, @Nullable Configuration replace) {
        if (tested == null || remove == null) {
            return;
        }
        Set<Configuration> cfg = new HashSet<>(tested.getExtendsFrom());
        if (cfg.remove(remove) && replace != null) {
            cfg.add(replace);
        }
        tested.setExtendsFrom(cfg);
    }

    private Configuration createJavaCPPConfiguration(Project project, IJavaCppExtension extension) {
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

//        project.getPlugins().withType(MavenPlugin.class, plugin -> {
//            Object conv = project.getConvention().getPlugins().get("maven");
//            if (conv instanceof MavenPluginConvention) {
//                ((MavenPluginConvention) conv).getConf2ScopeMappings().addMapping(MavenPlugin.COMPILE_PRIORITY, ret, Conf2ScopeMappingContainer.COMPILE);
//                Predicate<ResolvedDependency> isCpp = rd -> rd.getModule().getId().getName().equals("javacpp") && rd.getModule().getId().getGroup().equals("org.bytedeco");
//                fixMavenPom(project, () -> {
//                    List<ResolvedDependency> t = new ArrayList<>();
//                    compile.getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(rd -> {
//                        if (!isCpp.test(rd) || !rd.getModuleVersion().equals(JCPP_VERSION)) {
//                            addAll(rd, isCpp, t::add);
//                        }
//                    });
//                    return t.isEmpty() ? JCPP_VERSION : t.get(0).getModule().getId().getVersion();
//                });
//            }
//        });

        project.afterEvaluate(p -> {

            ret.defaultDependencies(dependencies -> {
                DomainObjectSet<Dependency> dep = compile.getAllDependencies().matching(element ->
                        element.getGroup() != null && element.getGroup().equals("org.bytedeco") && element.getName().equals("javacpp")
                );

                if (!dep.isEmpty()) {
                    dependencies.addAll(dep);
                } else {
                    dependencies.add(project.getDependencies().create("org.bytedeco:javacpp:" + extension.getDefaultJavaCPPVersion().get()));
                }
            });
        });
        return ret;
    }

    @Nonnull
    public static File getGeneratedCppFolder(Project project) {
        return new File(ProjectHelper.getBuildFolder(project), "tmp/generated/cpp");
    }

    @Nonnull
    public static File getGeneratedJavaFolder(Project project) {
        return new File(ProjectHelper.getBuildFolder(project), "generated/java");
    }

}
