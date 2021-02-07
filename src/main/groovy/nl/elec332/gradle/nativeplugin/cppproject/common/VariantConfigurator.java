package nl.elec332.gradle.nativeplugin.cppproject.common;

import nl.elec332.gradle.nativeplugin.util.NativeVariantHelper;
import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.language.cpp.CppLibrary;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 1/30/2021
 */
public class VariantConfigurator {

    public static final Attribute<Boolean> SHARED_RUNTIME = Attribute.of("sharedRuntime", Boolean.class);

    public static final String STATIC_RUNTIME_NAME = "staticRuntime";
    public static final Attribute<Boolean> STATIC_RUNTIME = Attribute.of(STATIC_RUNTIME_NAME, Boolean.class);

    public static void addSharedRuntimeVariant(Project project, CppLibrary component, Consumer<Runnable> callbacks) {
        NativeVariantHelper.addLibraryVariant("SharedRuntime", "-MD", project, component, binary -> {
            NativeVariantHelper.modifyOutgoingAttributes(project, binary, STATIC_RUNTIME, false);
            callbacks.accept(() -> binary.getCompileTask().get().getExtensions().getByType(ExtraPropertiesExtension.class).set(STATIC_RUNTIME_NAME, false));
        });
    }

    public static void addStaticRuntimeVariant(Project project, CppLibrary component, Consumer<Runnable> callbacks) {
        NativeVariantHelper.addLibraryVariant("StaticRuntime", "-MT", project, component, binary -> {
            NativeVariantHelper.modifyOutgoingAttributes(project, binary, STATIC_RUNTIME, true);
            callbacks.accept(() -> binary.getCompileTask().get().getExtensions().getByType(ExtraPropertiesExtension.class).set(STATIC_RUNTIME_NAME, true));
        });
    }

}
