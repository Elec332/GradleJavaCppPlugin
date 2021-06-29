package nl.elec332.gradle.nativeplugin.javacpp;

import nl.elec332.gradle.util.FileHelper;
import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.JavaPluginHelper;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.ComponentWithOutputs;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.language.nativeplatform.ComponentWithRuntimeUsage;
import org.gradle.nativeplatform.platform.Architecture;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.OperatingSystem;
import org.gradle.nativeplatform.platform.internal.ArchitectureInternal;
import org.gradle.nativeplatform.platform.internal.OperatingSystemInternal;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * Created by Elec332 on 2/20/2021
 */
public class PlatformHandler {

    @SuppressWarnings("UnstableApiUsage")
    public static <B extends CppBinary & ComponentWithOutputs & ComponentWithRuntimeUsage> void addPlatformJar(Project project, B binary, IJavaCppExtension extension, Task createTask) {
        NativePlatform platform = binary.getCompileTask().get().getTargetPlatform().get();
        String cppType = getFolderName(platform);
        SourceSet ss = JavaPluginHelper.childSet(project, JavaPluginHelper.getMainJavaSourceSet(project), cppType);
        String packageName = extension.getMainClass().get().substring(0, extension.getMainClass().get().lastIndexOf("."));
        String module = JavaPluginHelper.getModuleName(project);
        if (module != null) {
            String thisModule = module + "." + getOsName(platform.getOperatingSystem()) + "." + getArchName(platform.getArchitecture());

            File generatedJava = new File(JavaCppPlugin.getGeneratedJavaFolder(project), cppType);
            FileHelper.cleanFolder(generatedJava);

            File moduleInfo = new File(generatedJava, "module-info.java");
            GroovyHooks.inject(moduleInfo, "module " + thisModule + " {\n");
            GroovyHooks.inject(moduleInfo, "    requires " + module + ";\n");
            GroovyHooks.inject(moduleInfo, "}\n");

            ss.getJava().srcDir(generatedJava);
        }

        ProcessResources resources = JavaPluginHelper.getResourcesTask(project, ss);
        resources.dependsOn(createTask);
        resources.into(packageName.replace(".", "/") + "/" + cppType, copySpec -> {
            copySpec.from(binary.getOutputs());
            copySpec.from(binary.getRuntimeLibraries());
            copySpec.include(f -> f.getName().endsWith(((OperatingSystemInternal) platform.getOperatingSystem()).getInternalOs().getSharedLibrarySuffix()));
        });

        System.out.println(resources.getDestinationDir());
        project.getTasks().withType(JavaExec.class, a -> {
            a.dependsOn(resources);
            a.classpath(resources.getDestinationDir());
        });

        Task jar = project.getTasks().getByName("jar");
        TaskProvider<Jar> platformJarTask = project.getTasks().register(cppType + "Jar", Jar.class, task -> {
            task.getArchiveClassifier().set(cppType);
            task.from(ss.getOutput());
            task.dependsOn(ss.getCompileJavaTaskName());
        });

        jar.dependsOn(platformJarTask);

    }

    @Nonnull
    public static String getFolderName(NativePlatform targetPlatform) {
        return getOsName(targetPlatform.getOperatingSystem()) + "-" + getArchName(targetPlatform.getArchitecture());
    }

    public static String getOsName(OperatingSystem os) {
        String osName = "<error>";
        if (os.isWindows()) {
            osName = "windows";
        } else if (os.isLinux()) {
            osName = "linux";
        } else if (os.isMacOsX()) {
            osName = "macosx";
        } else if (os.isFreeBSD() || os.isSolaris()) {
            throw new UnsupportedOperationException();
        }
        return osName;
    }

    public static String getArchName(Architecture architecture) {
        ArchitectureInternal platform = (ArchitectureInternal) architecture;

        String archName;
        if (platform.isAmd64()) {
            archName = "x86_64";
        } else if (platform.isArm()) {
            archName = "armhf";
        } else if (platform.isI386()) {
            archName = "x86";
        } else if (platform.isIa64()) { //Get out
            throw new UnsupportedOperationException();
        } else {
            if (platform.getName().contains("arm-")) {
                archName = "arm64";
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return archName;
    }

}
