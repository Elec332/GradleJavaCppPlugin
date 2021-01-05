package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.util.GroovyHooks;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.component.PublishableComponent;
import org.gradle.language.ComponentWithOutputs;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppExecutable;
import org.gradle.language.cpp.CppSharedLibrary;
import org.gradle.language.cpp.CppStaticLibrary;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.language.nativeplatform.ComponentWithLinkUsage;
import org.gradle.language.nativeplatform.ComponentWithRuntimeUsage;
import org.gradle.nativeplatform.tasks.AbstractLinkTask;
import org.gradle.nativeplatform.tasks.CreateStaticLibrary;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Elec332 on 28-12-2020
 */
@SuppressWarnings("UnstableApiUsage")
public class BinaryConfigurator {

    static <B extends CppBinary & ComponentWithOutputs & ComponentWithRuntimeUsage & PublishableComponent> void configureBinary(Project project, NativeProjectExtension nativeProject, B binary) {
        mergeConfigurations(project, binary);
        checkIncludes(nativeProject, binary);
    }

    static void configureExecutableBinary(Project project, NativeProjectExtension nativeProject, CppExecutable binary) {
        addDependencies(binary, binary.getLinkTask().get(), nativeProject);
    }

    static<B extends CppBinary & ComponentWithLinkUsage & ComponentWithOutputs & ComponentWithRuntimeUsage & PublishableComponent> void configureLibraryBinary(Project project, NativeProjectExtension nativeProject, B binary) {

    }

    static void configureSharedLibraryBinary(Project project, NativeProjectExtension nativeProject, CppSharedLibrary binary) {
        addDependencies(binary, binary.getLinkTask().get(), nativeProject);
    }

    static void configureStaticLibraryBinary(Project project, NativeProjectExtension nativeProject, CppStaticLibrary binary) {
        mergeStaticLibraries(project, binary);
    }

    /////////////////////////////////////////////////////////////////

    private static void addDependencies(CppBinary binary, AbstractLinkTask task, NativeProjectExtension nativeProject) {
        if (!binary.getTargetPlatform().getTargetMachine().getOperatingSystemFamily().isWindows()) {
            task.getLinkerArgs().addAll(nativeProject.getDepHandler().getDefaultLinuxIncludes());
            task.getLinkerArgs().addAll(nativeProject.getDepHandler().getLinLink());
            for (String s : nativeProject.getDepHandler().getLinuxPkgDeps()) {
                String[] libs = GroovyHooks.processOutputToText(GroovyHooks.createProcess("pkg-config --libs " + s)).trim().split(" ");
                task.getLinkerArgs().addAll(libs);
            }
        } else {
            task.getLinkerArgs().addAll(nativeProject.getDepHandler().getDefaultWindowsIncludes());
            task.getLinkerArgs().addAll(nativeProject.getDepHandler().getWinLink());
        }
    }

    private static void mergeStaticLibraries(Project project, CppStaticLibrary binary) {
        Set<File> deps = project.getConfigurations().getAt(AbstractNativePlugin.STATIC_LINKER).resolve();
        if (binary.getTargetPlatform().getTargetMachine().getOperatingSystemFamily().isWindows()) {
            deps.forEach(file -> ((CreateStaticLibrary) binary.getLinkFileProducer().get()).source(project.files(file)));
        } else {
            binary.getCreateTask().get().finalizedBy(LinuxHelper.createStaticMergeTask(project, binary, binary.getLinkFile().get().getAsFile(), deps.stream().map(File::getAbsolutePath).toArray(String[]::new)));
        }
    }

    private static void mergeConfigurations(Project project, CppBinary binary) {
        ((Configuration) binary.getLinkLibraries()).extendsFrom(project.getConfigurations().getByName(AbstractNativePlugin.LINKER));
        ((Configuration) binary.getRuntimeLibraries()).extendsFrom(project.getConfigurations().getByName(AbstractNativePlugin.DYNAMIC));

        binary.getCompileTask().get().includes(project.getConfigurations().getByName(AbstractNativePlugin.HEADERS));
        if (binary.getTargetPlatform().getTargetMachine().getOperatingSystemFamily().isWindows()) {
            binary.getCompileTask().get().includes(project.getConfigurations().getByName(AbstractNativePlugin.WINDOWS_HEADERS));
        }
    }

    private static void checkIncludes(NativeProjectExtension nativeProject, CppBinary binary) {
        Set<File> all = new HashSet<>();
        binary.getCompileTask().get().getIncludes().forEach(all::add);
        binary.getCompileTask().get().getSystemIncludes().forEach(all::add);
        Set<String> remove = new HashSet<>();
        for (String s : nativeProject.getHeadersFound()) {
            Iterator<File> it = all.iterator();
            boolean found = false;
            while (!found && it.hasNext()) {
                File f = it.next();
                if (new File(f, s).exists()) {
                    found = true;
                }
            }
            if (found) {
                remove.add(s);
            }
        }
        nativeProject.getHeadersFound().removeAll(remove);
        remove.clear();
    }

}
