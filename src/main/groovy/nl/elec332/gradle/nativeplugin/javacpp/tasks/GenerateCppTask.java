package nl.elec332.gradle.nativeplugin.javacpp.tasks;

import nl.elec332.gradle.nativeplugin.javacpp.IJavaCppExtension;
import nl.elec332.gradle.nativeplugin.javacpp.JavaCppPlugin;
import nl.elec332.gradle.util.FileHelper;
import nl.elec332.gradle.util.JavaPluginHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;
import java.io.File;

/**
 * Created by Elec332 on 2/20/2021
 */
public class GenerateCppTask extends DefaultTask {

    @Inject
    public GenerateCppTask(Configuration cfg, IJavaCppExtension extension) {
        this.cppConfig = cfg;
        this.extension = extension;

        dependsOn(JavaPluginHelper.getClassesTask(getProject()));
    }

    private final Configuration cppConfig;
    private final IJavaCppExtension extension;

    @TaskAction
    public void generateCPPFiles() {
        Project project = getProject();
//        project.files(Arrays.stream(new File(gr + "/lib").listFiles()).filter(f -> f.isFile()).collect(Collectors.thisoList())toList)
        File dest = JavaCppPlugin.getGeneratedCppFolder(project);
        JavaCompile jc = JavaPluginHelper.getJavaCompileTask(getProject());
        String mainClass = extension.getMainClass().get();
        FileHelper.cleanFolder(dest);
        System.out.println("Generating source for: " + mainClass);
        getProject().javaexec(javaExecSpec -> {
            javaExecSpec.setMain("org.bytedeco.javacpp.tools.Builder");
            javaExecSpec.classpath(cppConfig.getAsPath());
            javaExecSpec.args("-cp", jc.getDestinationDir().getAbsolutePath(),
                    "-nocompile",
                    "-d", dest,
                    mainClass.replace(".", "/"));
        });
    }

}
