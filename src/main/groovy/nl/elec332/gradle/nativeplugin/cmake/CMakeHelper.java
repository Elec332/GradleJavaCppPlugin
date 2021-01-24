package nl.elec332.gradle.nativeplugin.cmake;

import nl.elec332.gradle.nativeplugin.cmake.tasks.CMakeBuildTask;
import nl.elec332.gradle.nativeplugin.cmake.tasks.CMakeSetupTask;
import nl.elec332.gradle.nativeplugin.common.AbstractCppPlugin;
import nl.elec332.gradle.nativeplugin.common.Constants;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.nativeplatform.toolchain.NativeToolChainRegistry;
import org.gradle.nativeplatform.toolchain.VisualCpp;
import org.gradle.platform.base.ToolChain;
import org.gradle.process.ExecResult;
import org.gradle.process.ExecSpec;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    //Todo: Fix
    public static void registerCMakeProject(Project project, ICMakeSettings settings) {
        CMakeSetupTask setupTask = project.getTasks().create("setupCMake_" + settings.getName(), CMakeSetupTask.class, settings);
        CMakeBuildTask buildTaskRelease = project.getTasks().create("buildCMakeRelease_" + settings.getName(), CMakeBuildTask.class, settings, settings.getReleaseBuildType());
        buildTaskRelease.usingSetup(setupTask);
        CMakeBuildTask buildTaskDebug = project.getTasks().create("buildCMakeDebug_" + settings.getName(), CMakeBuildTask.class, settings, settings.getDebugBuildType());
        buildTaskDebug.usingSetup(setupTask);

        if (settings.getLinkerBinaries().isEmpty()) {
            settings.getLinkerBinaries().from(settings.getBuildDirectory().dir("lib/" + settings.getReleaseBuildType().get()).get().getAsFile().getAbsolutePath() + "/*.*");
        }
        if (settings.getRuntimeLibraries().isEmpty()) {
            settings.getRuntimeLibraries().from(settings.getBuildDirectory().dir("bin/" + settings.getReleaseBuildType().get()).get().getAsFile().getAbsolutePath() + "/*.*");
        }
        project.getTasks().getByName("build").dependsOn(buildTaskRelease);
    }

    public static void registerCMakeDependency(Project project, String name, Action<? super ICMakeSettings> modifier) {
        ICMakeSettings settings = newSettings(project, name,null);
        modifier.execute(settings);
        CMakeSetupTask setupTask = project.getTasks().create("setupCMake_" + settings.getName(), CMakeSetupTask.class, settings);
        CMakeBuildTask buildTaskRelease = project.getTasks().create("buildCMakeRelease_" + settings.getName(), CMakeBuildTask.class, settings, settings.getReleaseBuildType());
        buildTaskRelease.usingSetup(setupTask);
        CMakeBuildTask buildTaskDebug = project.getTasks().create("buildCMakeDebug_" + settings.getName(), CMakeBuildTask.class, settings, settings.getDebugBuildType());
        buildTaskDebug.usingSetup(setupTask);
        ConfigurableFileCollection headers = project.files(settings.getIncludeDirectory());
        if (settings.getLinkerBinaries().isEmpty()) {
            settings.getLinkerBinaries().from(settings.getBuildDirectory().dir("lib/" + settings.getReleaseBuildType().get()).get().getAsFile().getAbsolutePath() + "/*.*");
        }
        if (settings.getRuntimeLibraries().isEmpty()) {
            settings.getRuntimeLibraries().from(settings.getBuildDirectory().dir("bin/" + settings.getReleaseBuildType().get()).get().getAsFile().getAbsolutePath() + "/*.*");
        }
        project.getDependencies().add(AbstractCppPlugin.HEADERS, headers);
        project.getDependencies().add(AbstractCppPlugin.LINKER, settings.getLinkerBinaries());
        project.getDependencies().add(AbstractCppPlugin.DYNAMIC, settings.getRuntimeLibraries());
        headers.builtBy(setupTask);
        settings.getLinkerBinaries().builtBy(buildTaskRelease);
        settings.getRuntimeLibraries().builtBy(buildTaskRelease);
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
