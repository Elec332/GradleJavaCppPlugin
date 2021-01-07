package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.util.GroovyHooks;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Created by Elec332 on 5-1-2021
 */
public class HeaderGenerator {

    public static void generateHeaders(File rootFolder, NativeProjectExtension extension) {
        File file = new File(rootFolder, extension.getGeneratedHeaderSubFolder().get());
        file.mkdirs();
        generateIncludeHeader(file, extension);
        generateExportHeader(file, extension);
    }

    private static void generateIncludeHeader(File subFolder, NativeProjectExtension extension) {
        File file = new File(subFolder, "config.h");
        if (file.exists()) {
            file.delete();
        }
        extension.getHeadersFound().forEach(h -> {
            GroovyHooks.inject(file, "#define " + extension.getHeaderIncludeCheck().get(h) + "\n");
        });
        GroovyHooks.inject(file, "");
    }

    private static void generateExportHeader(File subFolder, NativeProjectExtension extension) {
        File file = new File(subFolder, "export.h");
        if (file.exists()) {
            file.delete();
        }
        try {
            InputStream is = HeaderGenerator.class.getClassLoader().getResource("exportbase.h").openStream();
            new BufferedReader(new InputStreamReader(is)).lines().forEach(s -> {
                GroovyHooks.inject(file, s.replace("REPLACEME", extension.getGeneratedHeaderSubFolder().get().toUpperCase(Locale.ROOT)) + "\n");
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
