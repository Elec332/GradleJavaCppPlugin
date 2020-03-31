package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.cpp.CppSourceSet;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

/**
 * Created by Elec332 on 30-3-2020
 */
public class GenerateCPPTask extends DefaultTask {

    @Inject
    public GenerateCPPTask(Configuration cfg) {
        this.packs = new ArrayList<>();
        this.ccppl = new ArrayList<>();
        this.outLibs = new HashMap<>();
        this.cppConfig = cfg;
    }

    private final Configuration cppConfig;
    final List<String> packs, ccppl;
    final Map<String, Map<String, String>> outLibs;

    @TaskAction
    public void generateCPPFiles() {
        cleanGeneratedFolder();
        for (String nam : ccppl) {
            String[] prt = nam.split("#");
            getProject().javaexec(javaExecSpec -> {
                javaExecSpec.setMain("org.bytedeco.javacpp.tools.Builder");
                javaExecSpec.classpath(cppConfig.getAsPath());
                JavaCompile jc = JavaPluginHelper.getJavaCompileTask(getProject());
                javaExecSpec.args("-cp", jc.getDestinationDir().getAbsolutePath(),
                        "-nocompile",
                        "-d", ProjectHelper.getDefaultMainSourceFolderPath(this) + "/cpp/generated/" + prt[1],
                        prt[0]);
            });
        }
    }

    @SuppressWarnings("all")
    private void cleanGeneratedFolder() {
        File rootF = new File(ProjectHelper.getDefaultMainSourceFolder(this), "/cpp/generated");
        if (!rootF.isDirectory()) {
            throw new RuntimeException();
        }
        for (File file : rootF.listFiles()) {
            if (file.isDirectory()) {
                for (File file2 : file.listFiles()) {
                    file2.delete();
                }
            }
            file.delete();
        }
    }

    public void generateCPPCode(String jni_package, String jni_class, String libName, String... platforms) {
        addNativeInternalPre(jni_package, jni_class, libName, true, platforms);
    }

    public void generateExistingCPPCode(String jni_package, String jni_class, String libName, String... platforms) {
        addNativeInternalPre(jni_package, jni_class, libName, false, platforms);
    }

    private void addNativeInternalPre(String jni_package, String jni_class, String libName, boolean compInternal, String[] platforms) {
        String pack = jni_package.replace(".", "/");
        String jniDLL = "jni" + jni_class;
        packs.add(pack);
        ccppl.add(pack + "/" + jni_class + "#" + jniDLL);

        outLibs.computeIfAbsent(pack, k -> new HashMap<>());
        File folc = new File(ProjectHelper.getDefaultMainSourceFolder(this), "/cpp/generated/" + libName);
        if (!folc.exists()) {
            folc.mkdirs();
        }
        addNativeInternal(libName, jniDLL, pack, compInternal, platforms);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void addNativeInternal(String libName, String jniLibName, String pack, boolean compInternal, String[] platforms) {
        if (compInternal) {
            GroovyHooks.addModelComponent(getProject(), libName, spec -> {
                for (String platform : platforms) {
                    spec.targetPlatform(platform);
                }
                CppSourceSet cpp = NativeDevelopmentHelper.getCppSourceSet(spec);
                cpp.getSource().srcDir(ProjectHelper.getDefaultMainSourceFolderPath(this) + "/c/" + libName);
                cpp.getSource().srcDir(ProjectHelper.getDefaultMainSourceFolderPath(this) + "/c/common");
                addHeaders(cpp.getExportedHeaders(), pack, libName);
            }, binarySpec -> {
                CompilerArguments.setArgs(getProject(), binarySpec, binarySpec.getcCompiler(), false, null);
                if (binarySpec.getDisplayName().startsWith("shared")) {
                    outLibs.get(pack).put(binarySpec.getPrimaryOutput().getAbsolutePath(), NativeDevelopmentHelper.getFolderName(binarySpec.getTargetPlatform()));
                    String name = binarySpec.getName();
                    String upName = name.substring(0, 1).toUpperCase() + name.substring(1);
                    ProjectHelper.getTaskByName(getProject(), NativePlugin.COMPILE_C_PRE_TASK).dependsOn(libName + upName);
                    ProjectHelper.getTaskByName(getProject(), NativePlugin.COMPILE_C_TASK).dependsOn(jniLibName + upName);
                }
            });
        }

        GroovyHooks.addModelComponent(getProject(), jniLibName, spec -> {
            for (String platform : platforms) {
                spec.targetPlatform(platform);
            }
            CppSourceSet cpp = NativeDevelopmentHelper.getCppSourceSet(spec);
            cpp.getSource().srcDir(ProjectHelper.getDefaultMainSourceFolderPath(this) + "/cpp/generated/" + jniLibName);
            addHeaders(cpp.getExportedHeaders(), pack, libName);
        }, binarySpec -> {
            if (binarySpec.getDisplayName().startsWith("shared")) {
                outLibs.get(pack).put(binarySpec.getPrimaryOutput().getAbsolutePath(), NativeDevelopmentHelper.getFolderName(binarySpec.getTargetPlatform()));
            }

            String linkName = binarySpec.getPrimaryOutput().getAbsolutePath()
                    .replace(ProjectHelper.getProjectDirPath(this) + File.separator, "")
                    .replace(jniLibName, libName);
            linkName = linkName.substring(0, linkName.lastIndexOf(libName)) + libName + NativeDevelopmentHelper.getLibName(binarySpec.getTargetPlatform());
            CompilerArguments.setArgs(getProject(), binarySpec, binarySpec.getCppCompiler(), true, linkName);
        });
    }

    private void addHeaders(SourceDirectorySet srcManager, String pack, String libName) {
        NativeDependencies natDeps = NativePlugin.getNativeDependencies(getProject());
        srcManager.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(this) + "/c/" + libName + "/headers");
        srcManager.srcDir(ProjectHelper.getDefaultMainSourceFolderPath(this) + "/c/common/headers");
        for (String p : new String[]{"", pack}) {
            for (String s : natDeps.getLibs(p)) {
                srcManager.srcDir(natDeps.libRootFolder + "/" + s + "/include");
            }
        }
    }

}
