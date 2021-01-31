package nl.elec332.gradle.nativeplugin.cmake.util;

import nl.elec332.gradle.nativeplugin.api.cmake.ICMakeSettings;
import nl.elec332.gradle.nativeplugin.base.CppUtilsPlugin;
import nl.elec332.gradle.nativeplugin.cmake.tasks.CMakeBuildTask;
import nl.elec332.gradle.nativeplugin.cmake.tasks.CMakeSetupTask;
import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.nativeplatform.toolchain.NativeToolChainRegistry;
import org.gradle.nativeplatform.toolchain.VisualCpp;
import org.gradle.platform.base.ToolChain;
import org.gradle.process.ExecResult;
import org.gradle.process.ExecSpec;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

/**
 * Created by Elec332 on 1/23/2021
 */
public class CMakeHelper {

    private static final String NO_CMAKE = "_NO_CMAKE_GIVEN_";

    public static ICMakeSettings newSettingsExtension(Project project) {
        return project.getExtensions().create(ICMakeSettings.class, "cmake", CMakeSettings.class, project, project.getProjectDir(), "main", project.getLayout().getBuildDirectory().dir("cmake"));
    }

    public static ICMakeSettings newSettings(Project project, String name, @Nullable File file) {
        if (file == null) {
            return project.getExtensions().create(ICMakeSettings.class, "cmake_" + name, CMakeSettings.class, project, Constants.NULL_FILE, name, project.getLayout().getBuildDirectory().dir("cmake/" + name));
        } else {
            return project.getExtensions().create(ICMakeSettings.class, "cmake_" + name, CMakeSettings.class, project, file, name, project.getLayout().getBuildDirectory().dir("cmake/" + name));
        }
    }

    public static void registerCMakeProject(Project project, ICMakeSettings settings, Configuration linkRelease, Configuration runtimeRelease, Configuration linkDebug, Configuration runtimeDebug) {
        CMakeSetupTask setupTask = project.getTasks().create("setupCMake_" + settings.getName(), CMakeSetupTask.class, settings);
        CMakeBuildTask buildTaskRelease = project.getTasks().create("buildCMakeRelease_" + settings.getName(), CMakeBuildTask.class, settings, settings.getReleaseBuildType());
        buildTaskRelease.usingSetup(setupTask);
        CMakeBuildTask buildTaskDebug = project.getTasks().create("buildCMakeDebug_" + settings.getName(), CMakeBuildTask.class, settings, settings.getDebugBuildType());
        buildTaskDebug.usingSetup(setupTask);
        project.afterEvaluate(p -> configureBinaries(settings));
        project.getTasks().getByName("build").dependsOn(buildTaskRelease);
        project.afterEvaluate(p -> {
            registerArtifacts(project, settings.getReleaseBuildType(), settings.getReleaseLinkerBinaries(), linkRelease, buildTaskRelease);
            registerArtifacts(project, settings.getReleaseBuildType(), settings.getReleaseRuntimeBinaries(), runtimeRelease, buildTaskRelease);
            registerArtifacts(project, settings.getDebugBuildType(), settings.getDebugLinkerBinaries(), linkDebug, buildTaskDebug);
            registerArtifacts(project, settings.getDebugBuildType(), settings.getDebugRuntimeBinaries(), runtimeDebug, buildTaskDebug);
        });
    }

    private static void registerArtifacts(Project project, Property<String> name, ConfigurableFileCollection input, Configuration output, Object... tasks) {
        Iterator<File> files = input.getFiles().iterator();
        int dep = 1;
        while (files.hasNext()) {
            output.getOutgoing().artifact(new CMakeArtifact(project, name.get() + dep, files.next(), tasks));
        }
    }

    public static void registerCMakeDependency(Project project, String name, Action<? super ICMakeSettings> modifier) {
        ICMakeSettings settings = newSettings(project, name, null);
        modifier.execute(settings);
        CMakeSetupTask setupTask = project.getTasks().create("setupCMake_" + settings.getName(), CMakeSetupTask.class, settings);
        CMakeBuildTask buildTaskRelease = project.getTasks().create("buildCMakeRelease_" + settings.getName(), CMakeBuildTask.class, settings, settings.getReleaseBuildType());
        buildTaskRelease.usingSetup(setupTask);
        CMakeBuildTask buildTaskDebug = project.getTasks().create("buildCMakeDebug_" + settings.getName(), CMakeBuildTask.class, settings, settings.getDebugBuildType());
        buildTaskDebug.usingSetup(setupTask);

        project.afterEvaluate(p -> configureBinaries(settings));

        ConfigurableFileCollection headers = project.files(settings.getIncludeDirectory());
        project.getDependencies().add(CppUtilsPlugin.HEADERS, headers);
        project.getDependencies().add(CppUtilsPlugin.LINKER_RELEASE, settings.getReleaseLinkerBinaries());
        project.getDependencies().add(CppUtilsPlugin.RUNTIME_RELEASE, settings.getReleaseRuntimeBinaries());
        project.getDependencies().add(CppUtilsPlugin.LINKER_DEBUG, settings.getDebugLinkerBinaries());
        project.getDependencies().add(CppUtilsPlugin.RUNTIME_DEBUG, settings.getDebugRuntimeBinaries());
        headers.builtBy(setupTask);
        settings.getReleaseLinkerBinaries().builtBy(buildTaskRelease);
        settings.getReleaseRuntimeBinaries().builtBy(buildTaskRelease);
        settings.getDebugLinkerBinaries().builtBy(buildTaskDebug);
        settings.getDebugRuntimeBinaries().builtBy(buildTaskDebug);
    }

    private static void configureBinaries(ICMakeSettings settings) {
        configureOne(settings, settings.getReleaseLinkerBinaries(), settings.getLinkerBinaries(), settings.getReleaseBuildType());
        configureOne(settings, settings.getReleaseRuntimeBinaries(), settings.getRuntimeBinaries(), settings.getReleaseBuildType());
        configureOne(settings, settings.getDebugLinkerBinaries(), settings.getLinkerBinaries(), settings.getDebugBuildType());
        configureOne(settings, settings.getDebugRuntimeBinaries(), settings.getRuntimeBinaries(), settings.getDebugBuildType());
    }

    private static void configureOne(ICMakeSettings settings, ConfigurableFileCollection binaries, SetProperty<String> includer, Property<String> buildType) {
        if (binaries.isEmpty() || includer.isPresent()) {

            //TODO: SetProperties are always present, fix or report to gradle...
            Collection<String> extra;
            if (binaries.isEmpty()) {
                if (includer.isPresent() && !includer.get().isEmpty()) {
                    extra = includer.get();
                } else {
                    extra = Collections.singleton("*.*");
                }
            } else if (includer.isPresent()) {
                extra = includer.get();
            } else {
                extra = Collections.emptyList();
            }

            //Collection<String> extra = includer.isPresent() ? includer.get() : Collections.singleton("*.*");
            extra.forEach(s -> binaries.from(settings.getBuildDirectory().dir("lib/" + buildType.get()).get().getAsFile().getAbsolutePath() + "/" + s));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static ExecResult startCMake(Project project, ICMakeSettings settings, Action<? super ExecSpec> action) {
        String s = settings.getExecutable().getOrElse(NO_CMAKE);
        NativeToolChainRegistry registry = Utils.realizeToolChainRegistry(project);
        List<String> cmdLine = new ArrayList<>();
        if (s.equals(NO_CMAKE)) {
            for (ToolChain toolChain : registry) {
                if (toolChain instanceof VisualCpp) {
                    String path = new File(((VisualCpp) toolChain).getInstallDir(), "Common7/Tools/VsDevCmd.bat").getAbsolutePath();
                    cmdLine.addAll(Arrays.asList("cmd", "/c", path, "&"));
                    break;
                }
            }
            cmdLine.add("cmake");
        } else {
            cmdLine.add(s);
        }
        return project.exec(spec -> {
            spec.commandLine(cmdLine.toArray());
            action.execute(spec);
        });
    }

}
