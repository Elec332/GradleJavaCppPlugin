package nl.elec332.gradle.nativeplugin;

import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.internal.jvm.Jvm;
import org.gradle.nativeplatform.NativeLibrarySpec;
import org.gradle.nativeplatform.PreprocessingTool;
import org.gradle.nativeplatform.Tool;
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal;
import org.gradle.nativeplatform.platform.NativePlatform;

import java.io.File;

/**
 * Created by Elec332 on 31-3-2020
 */
@SuppressWarnings("UnstableApiUsage")
public class CompilerArguments {

    public static void setArgs(Project project, NativeBinarySpecInternal binarySpec, PreprocessingTool compiler, boolean isCpp, String libName_) {
        setArgs(project, binarySpec.getLinker(), compiler, binarySpec.getTargetPlatform(), NativeDevelopmentHelper.getCppSourceSet((NativeLibrarySpec) binarySpec.getComponent()).getExportedHeaders(), isCpp, libName_);
    }

    public static void setArgs(Project project, Tool linker, PreprocessingTool compiler, NativePlatform targetPlatform, SourceDirectorySet headers, boolean cpp, String libName_) {
        String javaHome = Jvm.current().getJavaHome().getAbsolutePath();
        if (targetPlatform.getOperatingSystem().isMacOsX()) {
            headers.srcDir(javaHome + "/include");
            headers.srcDir(javaHome + "/include/darwin");
            compiler.args("-mmacosx-version-min=10.4");
            linker.args("-mmacosx-version-min=10.4");
        } else if (targetPlatform.getOperatingSystem().isLinux()) {
            headers.srcDir(javaHome + "/include");
            headers.srcDir(javaHome + "/include/linux");
            compiler.args("-D_FILE_OFFSET_BITS=64");
        } else if (targetPlatform.getOperatingSystem().isWindows()) {
            headers.srcDir(javaHome + "/include");
            headers.srcDir(javaHome + "/include/win32");
            linker.args("Shlwapi.lib", "Advapi32.lib");
        } else if (targetPlatform.getOperatingSystem().isFreeBSD()) {
            headers.srcDir(javaHome + "/include");
            headers.srcDir(javaHome + "/include/freebsd");
        }

        compiler.args("-std=c++0x");
        compiler.args("-pthread");

        NativeDependencies natDeps = NativePlugin.getNativeDependencies(project);
        for (String s : natDeps.getAllLibs()) {
            String folderBase = natDeps.libRootFolder + File.separator + s + File.separator + NativeDevelopmentHelper.getFolderName(targetPlatform);
            if (new File(folderBase).exists()) {
                linker.args(folderBase + File.separator + s + NativeDevelopmentHelper.getLibName(targetPlatform));
            }
        }
//        for (String s : extraLibz.keySet()) {
//            File folder = new File(s + "/" + extraLibz.get(s)[1]);
//            for (File fil : folder.listFiles()) {
//                if (fil.getAbsolutePath().endsWith(getLibName(targetPlatform))) {
//                    linker.args(fil.getAbsolutePath());
//                }
//            }
//        }
        if (cpp) {
            linker.args(libName_);
        }
    }

}
