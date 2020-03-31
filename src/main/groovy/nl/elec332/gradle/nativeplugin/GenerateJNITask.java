package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.util.JavaPluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by Elec332 on 31-3-2020
 */
public class GenerateJNITask extends DefaultTask {

    @TaskAction
    @SuppressWarnings("all")
    public void copyNativeFiles() throws IOException {
        System.out.println("Copying native files");
        JavaCompile jc = JavaPluginHelper.getJavaCompileTask(getProject());
        File destinationDir = jc.getDestinationDir();
        NativeDependencies natDeps = NativePlugin.getNativeDependencies(getProject());
        GenerateCPPTask cpp = (GenerateCPPTask) Objects.requireNonNull(ProjectHelper.getTaskByName(getProject(), NativePlugin.GENERATE_CPP_TASK));
        for (String pack : cpp.packs) {
            Map<String, String> mawp = cpp.outLibs.get(pack);
            if (mawp == null) {
                mawp = new HashMap<>();
            }
            for (String s : new HashSet<>(mawp.values())) {
                File destFolder = new File(destinationDir, pack + File.separator + s);
                if (destFolder.exists()) {
                    for (File file : destFolder.listFiles()) {
                        file.delete();
                    }
                    destFolder.delete();
                }
                destFolder.mkdirs();
            }
            Set<String> check = new HashSet<>();
            for (String s : mawp.keySet()) {
                String platform = mawp.get(s);
                String fileName = s.substring(s.lastIndexOf(File.separator) + 1);
                String ext = fileName.substring(fileName.lastIndexOf('.'));
                File destBase = new File(destinationDir, pack + File.separator + platform);
                File orig = new File(s);
                File dest = new File(destBase, fileName);
                Files.copy(orig.toPath(), dest.toPath());
                if (check.add(platform)) {
                    for (String cn : new String[]{"", pack}) {
                        for (String dep : natDeps.getLibs(cn)) {
                            File ori = new File(natDeps.libRootFolder, dep + File.separator + platform + File.separator + dep + ext);
                            if (!ori.exists()) {
                                continue;
                            }
                            File des = new File(destBase, dep + ext);
                            Files.copy(ori.toPath(), des.toPath());
                        }
                    }
                }
            }

        }
    }

}
