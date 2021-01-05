package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.util.GroovyHooks;
import org.gradle.api.Project;

import java.io.File;

/**
 * Created by Elec332 on 5-1-2021
 */
public class HeaderGenerator {

    public static void generateHeaders(Project project, File rootFolder, NativeProjectExtension extension) {
        File file = new File(rootFolder, extension.getGeneratedHeaderFolder().get());
        file.mkdirs();
        generateIncludeHeader(file, extension);
    }

    private static void generateIncludeHeader(File subFolder, NativeProjectExtension extension) {
        File file = new File(subFolder, "include.h");
        if (file.exists()) {
            file.delete();
        }
        extension.getHeadersFound().forEach(h -> {
            GroovyHooks.inject(file, "#define " + extension.getHeaderIncludeCheck().get(h) + "\n");
        });
        GroovyHooks.inject(file, "");
    }

}
