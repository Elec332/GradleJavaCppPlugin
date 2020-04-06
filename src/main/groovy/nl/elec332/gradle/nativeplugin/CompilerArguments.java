package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.util.JavaPluginHelper;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.nativeplatform.NativeBinarySpec;
import org.gradle.nativeplatform.NativeLibrarySpec;
import org.gradle.nativeplatform.PreprocessingTool;
import org.gradle.nativeplatform.Tool;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.OperatingSystem;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 31-3-2020
 */
@SuppressWarnings("UnstableApiUsage")
public class CompilerArguments {

    public static void setArgs(Project project, NativeBinarySpec binarySpec, PreprocessingTool compiler, BiConsumer<NativePlatform, Consumer<String>> libs) {
        setArgs(project, binarySpec.getLinker(), compiler, binarySpec.getTargetPlatform(), NativeDevelopmentHelper.getCppSourceSet((NativeLibrarySpec) binarySpec.getComponent()).getExportedHeaders(), libs);
    }

    public static void setArgs(Project project, Tool linker, PreprocessingTool compiler, NativePlatform targetPlatform, SourceDirectorySet headers, BiConsumer<NativePlatform, Consumer<String>> libs) {
        OperatingSystem os = targetPlatform.getOperatingSystem();

        if (project.getPluginManager().hasPlugin("java")) { //Java present
            String javaHome = JavaPluginHelper.getJavaHome();
            if (os.isMacOsX()) {
                headers.srcDir(javaHome + "/include");
                headers.srcDir(javaHome + "/include/darwin");
            } else if (os.isLinux()) {
                headers.srcDir(javaHome + "/include");
                headers.srcDir(javaHome + "/include/linux");
            } else if (os.isWindows()) {
                headers.srcDir(javaHome + "/include");
                headers.srcDir(javaHome + "/include/win32");
            } else if (os.isFreeBSD()) {
                headers.srcDir(javaHome + "/include");
                headers.srcDir(javaHome + "/include/freebsd");
            }
        }

        if (os.isMacOsX()) {
            compiler.args("-mmacosx-version-min=10.4");
            linker.args("-mmacosx-version-min=10.4");
        } else if (os.isLinux()) {
            compiler.args("-D_FILE_OFFSET_BITS=64");
        } else if (os.isWindows()) {
            linker.args("Shlwapi.lib", "Advapi32.lib");
        }

        compiler.args("-std=c++0x");
        compiler.args("-pthread");

        libs.accept(targetPlatform, linker::args);
    }

}
