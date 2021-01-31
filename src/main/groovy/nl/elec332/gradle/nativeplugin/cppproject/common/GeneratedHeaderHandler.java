package nl.elec332.gradle.nativeplugin.cppproject.common;

import nl.elec332.gradle.nativeplugin.cppproject.extensions.NativeProjectExtension;
import nl.elec332.gradle.util.GroovyHooks;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Objects;

/**
 * Created by Elec332 on 5-1-2021
 */
public class GeneratedHeaderHandler {

    public static void generateHeaders(File rootFolder, NativeProjectExtension extension) {
        File file = new File(rootFolder, extension.getGeneratedHeaderSubFolder().get());
        file.mkdirs();
        generateIncludeHeader(file, extension);
        generateExportHeader(file, extension);
        extension.modifyCompiler(compiler -> compiler.includes(rootFolder));
    }

    private static void generateIncludeHeader(File subFolder, NativeProjectExtension extension) {
        File file = new File(subFolder, "config.h");
        if (file.exists()) {
            file.delete();
        }
        if (!extension.getHeadersFound().isEmpty()) {
            extension.getHeadersFound().forEach(h -> {
                GroovyHooks.inject(file, "#define " + extension.getHeaderIncludeCheck().get(h) + "\n");
            });
            GroovyHooks.inject(file, "");
        }
    }

    private static void generateExportHeader(File subFolder, NativeProjectExtension extension) {
        File file = new File(subFolder, "export.h");
        if (file.exists()) {
            file.delete();
        }
        if (extension.getGenerateExportHeader().get()) {
            String name = extension.getGeneratedHeaderSubFolder().get().toUpperCase(Locale.ROOT);
            try {
                InputStream is = Objects.requireNonNull(GeneratedHeaderHandler.class.getClassLoader().getResource("exportbase.h")).openStream();
                new BufferedReader(new InputStreamReader(is)).lines().forEach(s -> {
                    GroovyHooks.inject(file, s.replace("REPLACEME", name) + "\n");
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            extension.modifyCompiler(compiler -> compiler.getCompilerArgs().add("-D" + name + "_CREATE_EXPORTS"));
        }
    }

}
