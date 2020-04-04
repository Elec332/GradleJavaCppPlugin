package nl.elec332.gradle.nativeplugin.javacpp;

import nl.elec332.gradle.nativeplugin.nativeproject.NativeSettings;
import nl.elec332.gradle.util.JavaPluginHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Created by Elec332 on 30-3-2020
 */
public class GenerateCPPTask extends DefaultTask {

    @Inject
    public GenerateCPPTask(Configuration cfg, Supplier<Collection<JNIProject>> jniProjects, NativeSettings settings) {
        this.cppConfig = cfg;
        this.jniProjects = jniProjects;
        settings.addCustomProjects(registry -> {
            for (JNIProject p : jniProjects.get()) {
                checkGeneratedFolder(p.name, false);
                registry.accept(p.name, project -> {
                    project.importFrom(p);
                    project.includeDefaultCCode = project.includeDefaultCPPCode = false;
                    project.source.srcDir(JavaCPPPlugin.getGeneratedCppFolder(getProject()).getAbsolutePath() + File.separator + p.name);
                    project.onConfigured(() -> checkGeneratedFolder(p.name, true));
                });
            }
        });
    }

    private final Configuration cppConfig;
    private final Supplier<Collection<JNIProject>> jniProjects;

    @TaskAction
    public void generateCPPFiles() {
        cleanGeneratedFolder();
        for (JNIProject project : jniProjects.get()) {
            getProject().javaexec(javaExecSpec -> {
                javaExecSpec.setMain("org.bytedeco.javacpp.tools.Builder");
                javaExecSpec.classpath(cppConfig.getAsPath());
                JavaCompile jc = JavaPluginHelper.getJavaCompileTask(getProject());
                System.out.println("Generating source for: " + project.getMainClassSlashes().replace("/", "."));
                javaExecSpec.args("-cp", jc.getDestinationDir().getAbsolutePath(),
                        "-nocompile",
                        "-d", JavaCPPPlugin.getGeneratedCppFolder(getProject()).getAbsolutePath() + File.separator + project.name,
                        project.getMainClassSlashes());
            });
        }
    }

    @SuppressWarnings("all")
    private void cleanGeneratedFolder() {
        File rootF = JavaCPPPlugin.getGeneratedCppFolder(getProject());
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkGeneratedFolder(String jniLibName, boolean deleteDummy) {
        File folc = new File(JavaCPPPlugin.getGeneratedCppFolder(getProject()), jniLibName);
        if (!folc.exists()) {
            folc.mkdirs();
            File f = new File(folc, "dummy.cpp");
            if (deleteDummy) {
                if (f.exists()) {
                    f.delete();
                }
            } else {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
