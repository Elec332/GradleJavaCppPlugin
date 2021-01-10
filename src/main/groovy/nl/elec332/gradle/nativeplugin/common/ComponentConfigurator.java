package nl.elec332.gradle.nativeplugin.common;

import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.CppLibrary;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 6-1-2021
 */
public class ComponentConfigurator {

    static void configureComponent(Project project, NativeProjectExtension nativeProject, CppComponent component, Consumer<Runnable> callbacks) {
        component.getPrivateHeaders().from("src/" + component.getName() + "/headers");
        component.getPrivateHeaders().from(nativeProject.getGeneratedHeadersDir());
    }

    static void configureLibrary(Project project, InternalHelper helper, NativeProjectExtension nativeProject, CppLibrary component, Consumer<Runnable> callbacks) {
        component.getLinkage().set(nativeProject.getLinkage());
        ((org.gradle.language.cpp.internal.DefaultCppLibrary) component).getApiElements().getOutgoing().artifact(nativeProject.getGeneratedHeadersDir());
        project.getTasks().configureEach(task -> {
            if (task instanceof Zip && task.getName().equals("cppHeaders")) {
                ((Zip) task).from(nativeProject.getGeneratedHeadersDir());
            }
        });


//        helper.addLibraryVariant("small", "-s", project, component, binary -> {
//
////            ((DefaultCppBinary) binary).getIncludePathConfiguration().attributes(a -> a.attribute(AbstractNativePlugin.SMALL_ATTRIBUTE, true));
////            ((Configuration) binary.getLinkLibraries()).attributes(a -> a.attribute(AbstractNativePlugin.SMALL_ATTRIBUTE, true));
////            ((Configuration) binary.getRuntimeLibraries()).attributes(a -> a.attribute(AbstractNativePlugin.SMALL_ATTRIBUTE, true));
//            helper.modifyAttributes(binary, AbstractNativePlugin.SMALL_ATTRIBUTE, true);
//            callbacks.accept(() -> {
//                binary.getCompileTask().get().getCompilerArgs().add("-MD");
//            });
//        });

    }

    static void configureExecutable(Project project, InternalHelper helper, NativeProjectExtension nativeProject, CppApplication component, Consumer<Runnable> callbacks) {
//        helper.addExecutableVariant("small", "-s", project, component, binary -> {
//            ((DefaultCppBinary) binary).getIncludePathConfiguration().attributes(a -> a.attribute(AbstractNativePlugin.SMALL_ATTRIBUTE, true));
//            ((Configuration) binary.getLinkLibraries()).attributes(a -> a.attribute(AbstractNativePlugin.SMALL_ATTRIBUTE, true));
//            ((Configuration) binary.getRuntimeLibraries()).attributes(a -> a.attribute(AbstractNativePlugin.SMALL_ATTRIBUTE, true));
//            helper.modifyAttributes(binary, AbstractNativePlugin.SMALL_ATTRIBUTE, true);
//            callbacks.accept(() -> {
//                binary.getCompileTask().get().getCompilerArgs().add("-MD");
//            });
//        });
    }

}
