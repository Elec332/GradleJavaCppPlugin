package nl.elec332.gradle.nativeplugin.cppproject.common;

import nl.elec332.gradle.nativeplugin.base.IBinaryConfigurator;
import nl.elec332.gradle.nativeplugin.cppproject.extensions.NativeProjectExtension;
import nl.elec332.gradle.util.GroovyHooks;
import org.gradle.api.Project;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppSharedLibrary;
import org.gradle.language.nativeplatform.ComponentWithExecutable;
import org.gradle.language.nativeplatform.ComponentWithInstallation;
import org.gradle.nativeplatform.tasks.AbstractLinkTask;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;
import org.gradle.nativeplatform.toolchain.VisualCpp;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Elec332 on 28-12-2020
 */
@SuppressWarnings("UnstableApiUsage")
public class BinaryConfigurator implements IBinaryConfigurator<NativeProjectExtension> {

    @Override
    public void configureBinary(Project project, CppBinary binary, NativeProjectExtension data) {
        StaticLibraryHandler.mergeStaticLibraries(project, binary);
        setCompilerParameters(data, binary);
        checkIncludes(data, binary);
    }

    @Override
    public <B extends CppBinary & ComponentWithExecutable & ComponentWithInstallation> void configureExecutableBinary(Project project, B binary, NativeProjectExtension data) {
        addDependencies(binary, binary.getLinkTask().get(), data);
    }

    @Override
    public void configureSharedLibraryBinary(Project project, CppSharedLibrary binary, NativeProjectExtension data) {
        addDependencies(binary, binary.getLinkTask().get(), data);
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


    private static void checkIncludes(NativeProjectExtension nativeProject, CppBinary binary) {
        if (nativeProject.getHeadersFound().isEmpty()) {
            return;
        }
        Set<File> all = new HashSet<>();
        binary.getCompileTask().get().getIncludes().forEach(all::add);
        binary.getCompileTask().get().getSystemIncludes().forEach(all::add);
        Set<String> remove = new HashSet<>();
        System.out.println(binary.getName());
        for (String s : nativeProject.getHeadersFound()) {
            Iterator<File> it = all.iterator();
            boolean found = false;
            while (!found && it.hasNext()) {
                File f = it.next();
                System.out.println(f);
                if (new File(f, s).exists()) {
                    found = true;
                }
            }
            if (!found) {
                remove.add(s);
            }
        }

        nativeProject.getHeadersFound().removeAll(remove);
        remove.clear();
    }

    private static void setCompilerParameters(NativeProjectExtension extension, CppBinary binary) {
        Set<String> extraArgs = new HashSet<>();

        if (!binary.isDebuggable()) {
            extraArgs.add("-DNDEBUG");
        }
        if (binary.getToolChain() instanceof VisualCpp) {
            String arg = "-";
            if (extension.getStaticRuntime().get() || (binary instanceof CppTestExecutable && extension.getStaticTestRuntime().get())) {
                arg += "MT";
            } else {
                arg += "MD";
            }
            if (binary.isDebuggable()) {
                arg += "d";
                extraArgs.add("/Od");
                extraArgs.add("/Ob0");
            }
            if (extension.getMinimizeSize().get()) {
                extraArgs.add("/O1");
            } else {
                extraArgs.add("/O2");
            }
            extraArgs.add(arg);
        } else {
            if (extension.getMinimizeSize().get()) {
                extraArgs.add("-Os");
            } else {
                extraArgs.add("-O3");
            }
            if (binary instanceof CppSharedLibrary) {
                extraArgs.add("-fPIC");
            }
        }
        binary.getCompileTask().get().getCompilerArgs().addAll(extraArgs);
    }

}
