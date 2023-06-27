package nl.elec332.gradle.nativeplugin.cmake.util;

import nl.elec332.gradle.nativeplugin.api.cmake.ICMakeSettings;
import nl.elec332.gradle.nativeplugin.base.CppUtilsPlugin;
import nl.elec332.gradle.nativeplugin.cmake.tasks.CMakeBuildTask;
import nl.elec332.gradle.nativeplugin.cmake.tasks.CMakeSetupTask;
import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.internal.os.OperatingSystem;
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
        CMakeSetupTask setupTaskRelease = project.getTasks().create("setupCMakeRelease_" + settings.getName(), CMakeSetupTask.class, settings, settings.getReleaseBuildType());
        CMakeBuildTask buildTaskRelease = project.getTasks().create("buildCMakeRelease_" + settings.getName(), CMakeBuildTask.class, settings, settings.getReleaseBuildType());
        buildTaskRelease.usingSetup(setupTaskRelease);
        CMakeSetupTask setupTaskDebug = project.getTasks().create("setupCMakeDebug_" + settings.getName(), CMakeSetupTask.class, settings, settings.getDebugBuildType());
        CMakeBuildTask buildTaskDebug = project.getTasks().create("buildCMakeDebug_" + settings.getName(), CMakeBuildTask.class, settings, settings.getDebugBuildType());
        buildTaskDebug.usingSetup(setupTaskDebug);
        Task setupCMakePre = project.getTasks().create("setupCmakePre_" + settings.getName());
        setupTaskRelease.dependsOn(setupCMakePre);
        setupTaskDebug.dependsOn(setupCMakePre);
        project.afterEvaluate(p -> configureBinaries(project, settings));

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

        CMakeSetupTask setupTaskRelease = project.getTasks().create("setupCMakeRelease_" + settings.getName(), CMakeSetupTask.class, settings, settings.getReleaseBuildType());
        CMakeBuildTask buildTaskRelease = project.getTasks().create("buildCMakeRelease_" + settings.getName(), CMakeBuildTask.class, settings, settings.getReleaseBuildType());
        buildTaskRelease.usingSetup(setupTaskRelease);
        CMakeSetupTask setupTaskDebug = project.getTasks().create("setupCMakeDebug_" + settings.getName(), CMakeSetupTask.class, settings, settings.getDebugBuildType());
        CMakeBuildTask buildTaskDebug = project.getTasks().create("buildCMakeDebug_" + settings.getName(), CMakeBuildTask.class, settings, settings.getDebugBuildType());
        buildTaskDebug.usingSetup(setupTaskDebug);
        Task setupCMakePre = project.getTasks().create("setupCmakePre_" + settings.getName());
        setupTaskRelease.dependsOn(setupCMakePre);
        setupTaskDebug.dependsOn(setupCMakePre);
        project.afterEvaluate(p -> configureBinaries(project, settings));

        ConfigurableFileCollection headers = project.files(settings.getIncludeDirectory());
        project.getDependencies().add(CppUtilsPlugin.HEADERS, headers);
        project.getDependencies().add(CppUtilsPlugin.LINKER_RELEASE, settings.getReleaseLinkerBinaries());
        project.getDependencies().add(CppUtilsPlugin.RUNTIME_RELEASE, settings.getReleaseRuntimeBinaries());
        project.getDependencies().add(CppUtilsPlugin.LINKER_DEBUG, settings.getDebugLinkerBinaries());
        project.getDependencies().add(CppUtilsPlugin.RUNTIME_DEBUG, settings.getDebugRuntimeBinaries());
        headers.builtBy(setupTaskRelease);
        settings.getReleaseLinkerBinaries().builtBy(buildTaskRelease);
        settings.getReleaseRuntimeBinaries().builtBy(buildTaskRelease);
        settings.getDebugLinkerBinaries().builtBy(buildTaskDebug);
        settings.getDebugRuntimeBinaries().builtBy(buildTaskDebug);
    }

    private static void configureBinaries(Project project, ICMakeSettings settings) {
        configureOne(project, settings, settings.getReleaseLinkerBinaries(), settings.getLinkerBinaries(), settings.getReleaseBuildType(), false);
        configureOne(project, settings, settings.getReleaseRuntimeBinaries(), settings.getRuntimeBinaries(), settings.getReleaseBuildType(), true);
        configureOne(project, settings, settings.getDebugLinkerBinaries(), settings.getLinkerBinaries(), settings.getDebugBuildType(), false);
        configureOne(project, settings, settings.getDebugRuntimeBinaries(), settings.getRuntimeBinaries(), settings.getDebugBuildType(), true);
    }

    private static void configureOne(Project project, ICMakeSettings settings, ConfigurableFileCollection binaries, SetProperty<String> includer, Property<String> buildType, boolean runtime) {
        boolean multiBuild = getVCPPInstallDir(project) != null;
        for (String s : includer.getOrElse(Collections.emptySet())) {
            String[] split = s.split(":");
            String file;
            String folder = null;
            if (split.length == 2) {
                OperatingSystem os = OperatingSystem.current();
                if (split[0].equals("static")) {
                    if (runtime) {
                        continue;
                    } else {
                        folder = "lib";
                        file = os.getStaticLibraryName(split[1]);
                    }
                } else if (split[1].equals("shared")) {
                    if (runtime) {
                        file = os.getSharedLibraryName(split[1]);
                    } else {
                        file = os.getLinkLibraryName(split[1]);
                    }
                } else {
                    throw new UnsupportedOperationException(split[0]);
                }
            } else {
                file = s;
            }
            if (folder == null) {
                if (runtime) {
                    folder = "lib";
                } else {
                    folder = "bin";
                }
            }
            if (multiBuild) {
                folder += "/" + buildType.get();
            }
            binaries.from(settings.getBuildDirectory().dir(buildType.get() + "/" + folder).get().file(file));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static File getVCPPInstallDir(Project project) {
        NativeToolChainRegistry registry = Utils.realizeToolChainRegistry(project);
        for (ToolChain toolChain : registry) {
            if (toolChain instanceof VisualCpp) {
                File installDir = ((VisualCpp) toolChain).getInstallDir();
                if (installDir != null) {
                    return new File(installDir, "Common7/Tools/VsDevCmd.bat");
                }
            }
        }
        return null;
    }

    public static ExecResult startCMake(Project project, ICMakeSettings settings, Action<? super ExecSpec> action) {
        String s = settings.getExecutable().getOrElse(NO_CMAKE);

        List<String> cmdLine = new ArrayList<>();
        if (s.equals(NO_CMAKE)) {
            File installDir = getVCPPInstallDir(project);
            if (installDir != null) {
                cmdLine.addAll(Arrays.asList("cmd", "/c", installDir.getAbsolutePath(), "&"));
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
