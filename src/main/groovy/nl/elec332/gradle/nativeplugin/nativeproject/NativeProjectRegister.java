package nl.elec332.gradle.nativeplugin.nativeproject;

import nl.elec332.gradle.nativeplugin.CompilerArguments;
import nl.elec332.gradle.nativeplugin.GroovyHooks;
import nl.elec332.gradle.nativeplugin.NativeDevelopmentHelper;
import org.gradle.api.Project;
import org.gradle.language.cpp.CppSourceSet;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;

/**
 * Created by Elec332 on 3-4-2020
 */
public class NativeProjectRegister {

    @SuppressWarnings("UnstableApiUsage")
    public static void registerNativeProject(final Project project, final NativeSettings settings, final NativeProject nativeProject, Function<String, NativeProject> allProjects) {
        GroovyHooks.addModelComponent(project, nativeProject.name, spec -> {
            for (String platform : nativeProject.platforms) {
                spec.targetPlatform(platform);
            }
            CppSourceSet cpp = NativeDevelopmentHelper.getCppSourceSet(spec);
            settings.getSource(cpp.getSource(), nativeProject);
            settings.getHeaders(cpp.getExportedHeaders(), nativeProject);
            for (String s : settings.getProjectDependencies(nativeProject)) {
                settings.getHeaders(cpp.getExportedHeaders(), Objects.requireNonNull(allProjects.apply(s)));
            }
        }, binarySpec -> {
            String output = binarySpec.getPrimaryOutput().getAbsolutePath();
            String ext = output.substring(output.lastIndexOf('.'));
            boolean addLib = binarySpec.getDisplayName().startsWith("shared") || !nativeProject.excludeStaticLibraryCollection;
            CompilerArguments.setArgs(binarySpec, binarySpec.getCppCompiler(), (platform, linker) -> {
                String libExt = NativeDevelopmentHelper.getLibName(platform);
                String platformName = NativeDevelopmentHelper.getFolderName(binarySpec.getTargetPlatform());

                for (String s : settings.getLocalDependencies(nativeProject)) {
                    String locationBase = settings.libRootFolder + File.separator + s + File.separator + NativeDevelopmentHelper.getFolderName(platform);
                    if (new File(locationBase).exists()) {
                        String fileLoc = locationBase + File.separator + s;
                        linker.accept(fileLoc + libExt);
                        if (addLib) {
                            nativeProject.localLibs.put(platformName, new File(fileLoc + ext));
                            nativeProject.localLinks.add(new File(fileLoc + libExt));
                        }
                    }
                }
                for (String s : settings.getProjectDependencies(nativeProject)) {
                    NativeProject dep = Objects.requireNonNull(allProjects.apply(s));
                    dep.localLinks.forEach(f -> linker.accept(f.getAbsolutePath()));
                    nativeProject.localLibs.putAll(dep.localLibs);
                }

                if (addLib) {
                    nativeProject.localLibs.put(platformName, new File(output));
                    nativeProject.localLinks.add(new File(output.replace(ext, libExt)));
                }
            });
            nativeProject.specCallback.forEach(c -> c.accept(binarySpec));
        });
    }

}
