package nl.elec332.gradle.nativeplugin.cppproject.common.varianttest;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppLibrary;
import org.gradle.language.cpp.internal.DefaultCppBinary;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 1/30/2021
 */
public class VariantConfigurator {

    public static final Attribute<Boolean> SMALL_ATTRIBUTE = Attribute.of("small", Boolean.class);

    static void addSmallVariant(Project project, InternalHelper helper, CppLibrary component, Consumer<Runnable> callbacks) {
        helper.addLibraryVariant("small", "-s", project, component, binary -> {
            ((DefaultCppBinary) binary).getIncludePathConfiguration().attributes(a -> a.attribute(SMALL_ATTRIBUTE, true));
            ((Configuration) binary.getLinkLibraries()).attributes(a -> a.attribute(SMALL_ATTRIBUTE, true));
            ((Configuration) binary.getRuntimeLibraries()).attributes(a -> a.attribute(SMALL_ATTRIBUTE, true));
            helper.modifyAttributes(binary, SMALL_ATTRIBUTE, true);
            callbacks.accept(() -> {
                binary.getCompileTask().get().getCompilerArgs().add("-MD");
            });
        });

    }

    static void addSmallVariant(Project project, InternalHelper helper, CppApplication component, Consumer<Runnable> callbacks) {
        helper.addExecutableVariant("small", "-s", project, component, binary -> {
            ((DefaultCppBinary) binary).getIncludePathConfiguration().attributes(a -> a.attribute(SMALL_ATTRIBUTE, true));
            ((Configuration) binary.getLinkLibraries()).attributes(a -> a.attribute(SMALL_ATTRIBUTE, true));
            ((Configuration) binary.getRuntimeLibraries()).attributes(a -> a.attribute(SMALL_ATTRIBUTE, true));
            helper.modifyAttributes(binary, SMALL_ATTRIBUTE, true);
            callbacks.accept(() -> {
                binary.getCompileTask().get().getCompilerArgs().add("-MD");
            });
        });
    }

}
