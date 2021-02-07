package nl.elec332.gradle.nativeplugin.util;

import nl.elec332.gradle.util.Utils;
import nl.elec332.gradle.util.abstraction.IProjectObjects;
import nl.elec332.gradle.util.internal.ProjectObjects;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.component.UsageContext;
import org.gradle.api.plugins.internal.AbstractUsageContext;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.language.cpp.*;
import org.gradle.language.cpp.internal.*;
import org.gradle.language.nativeplatform.ComponentWithLinkUsage;
import org.gradle.language.nativeplatform.ComponentWithRuntimeUsage;
import org.gradle.language.nativeplatform.internal.Dimensions;
import org.gradle.language.nativeplatform.internal.toolchains.ToolChainSelector;
import org.gradle.nativeplatform.Linkage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 2/7/2021
 */
@SuppressWarnings("UnstableApiUsage")
public class NativeVariantHelper {

    public static <T> void modifyIncomingAttributes(Project project, CppBinary binary, Attribute<T> key, T value) {
        IProjectObjects projectObjects = Utils.getProjectObjects(project);
        projectObjects.modifyAttributes(((Configuration) binary.getLinkLibraries()), key, value);
        projectObjects.modifyAttributes(((Configuration) binary.getRuntimeLibraries()), key, value);
    }

    public static <T> void modifyOutgoingAttributes(Project project, CppBinary binary, Attribute<T> key, T value) {
        IProjectObjects projectObjects = Utils.getProjectObjects(project);
        modifyAttributes(project, binary, key, value);
        if (binary instanceof ComponentWithRuntimeUsage) {
            projectObjects.modifyAttributes(((ComponentWithRuntimeUsage) binary).getRuntimeElements().getOrNull(), key, value);
        }
        if (binary instanceof ComponentWithLinkUsage) {
            projectObjects.modifyAttributes(((ComponentWithLinkUsage) binary).getLinkElements().getOrNull(), key, value);
        }
    }

    public static <T> void modifyAttributes(Project project, CppBinary binary, Attribute<T> key, T value) {
        NativeVariantIdentity identity = ((DefaultCppBinary) binary).getIdentity();
        if (!identity.getRuntimeUsageContext().getAttributes().contains(key)) {
            modifyAttributes(project, identity.getRuntimeUsageContext(), key, value);
            if (identity.getLinkUsageContext() != null) {
                modifyAttributes(project, identity.getLinkUsageContext(), key, value);
            }
        }
    }

    private static <T> void modifyAttributes(Project project, UsageContext context, Attribute<T> key, T value) {
        try {
            attributeSetter.invoke(context, Utils.getProjectObjects(project).concat((AttributeContainer) attributeGetter.invoke(context), key, value));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void addLibraryVariant(String name_, String fileSuffix, Project project, CppLibrary component, Consumer<CppBinary> binaryConsumer) {
        ProjectObjects projectObjects = (ProjectObjects) Utils.getProjectObjects(project);
        DefaultCppLibrary library = (DefaultCppLibrary) component;
        Provider<String> baseName = suffix(project, library.getBaseName(), fileSuffix);
        String name = "_" + name_.toLowerCase(Locale.ROOT);
        Dimensions.libraryVariants(baseName, library.getLinkage(), library.getTargetMachines(), project.getObjects(), projectObjects.attributesFactory,
                project.getProviders().provider(() -> project.getGroup().toString()), project.getProviders().provider(() -> project.getVersion().toString()),
                variantIdentity -> {
                    if (Dimensions.tryToBuildOnHost(variantIdentity)) {
                        ToolChainSelector.Result<CppPlatform> result = projectObjects.toolChainSelector.select(CppPlatform.class, new DefaultCppPlatform(variantIdentity.getTargetMachine()));
                        DefaultCppBinary binary;
                        setName(variantIdentity.getLinkUsageContext(), s -> s + name);
                        setName(variantIdentity.getRuntimeUsageContext(), s -> s + name);
                        if (variantIdentity.getLinkage().equals(Linkage.SHARED)) {
                            binary = project.getObjects().newInstance(DefaultCppSharedLibrary.class, library.getNames().append(variantIdentity.getName() + name), baseName, library.getCppSource(), library.getAllHeaderDirs(), library.getImplementationDependencies(), result.getTargetPlatform(), result.getToolChain(), result.getPlatformToolProvider(), variantIdentity);
                        } else {
                            binary = project.getObjects().newInstance(DefaultCppStaticLibrary.class, library.getNames().append(variantIdentity.getName() + name), baseName, library.getCppSource(), library.getAllHeaderDirs(), library.getImplementationDependencies(), result.getTargetPlatform(), result.getToolChain(), result.getPlatformToolProvider(), variantIdentity);
                        }
                        library.getBinaries().add(binary);
                        binaryConsumer.accept(binary);
                    } else {
                        // Known, but not buildable
                        library.getMainPublication().addVariant(variantIdentity);
                    }
                });
    }

    public static void addExecutableVariant(String name_, String fileSuffix, Project project, CppApplication component, Consumer<CppExecutable> binaryConsumer) {
        ProjectObjects projectObjects = (ProjectObjects) Utils.getProjectObjects(project);
        DefaultCppApplication application = (DefaultCppApplication) component;
        Provider<String> baseName = suffix(project, application.getBaseName(), fileSuffix);
        String name = "_" + name_.toLowerCase(Locale.ROOT);
        Dimensions.applicationVariants(baseName, application.getTargetMachines(), project.getObjects(), projectObjects.attributesFactory,
                project.getProviders().provider(() -> project.getGroup().toString()), project.getProviders().provider(() -> project.getVersion().toString()),
                variantIdentity -> {
                    if (Dimensions.tryToBuildOnHost(variantIdentity)) {
                        ToolChainSelector.Result<CppPlatform> result = projectObjects.toolChainSelector.select(CppPlatform.class, new DefaultCppPlatform(variantIdentity.getTargetMachine()));
                        CppExecutable executable = project.getObjects().newInstance(DefaultCppExecutable.class, application.getNames().append(variantIdentity.getName() + name), baseName, application.getCppSource(), application.getPrivateHeaderDirs(), application.getImplementationDependencies(), result.getTargetPlatform(), result.getToolChain(), result.getPlatformToolProvider(), variantIdentity);
                        application.getBinaries().add(executable);
                        binaryConsumer.accept(executable);
                    } else {
                        // Known, but not buildable
                        application.getMainPublication().addVariant(variantIdentity);
                    }
                });
    }

    private static Provider<String> suffix(Project project, Property<String> prop, String suf) {
        Property<String> ret = project.getObjects().property(String.class);
        ret.set(prop.get() + suf);
        return ret;
    }

    private static void setName(UsageContext context, Function<String, String> modifier) {
        try {
            nameSetter.invoke(context, modifier.apply((String) nameGetter.invoke(context)));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle nameGetter, nameSetter, attributeGetter, attributeSetter;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Field f = DefaultUsageContext.class.getDeclaredField("name");
            f.setAccessible(true);
            nameSetter = lookup.unreflectSetter(f);
            nameGetter = lookup.unreflectGetter(f);
            f = AbstractUsageContext.class.getDeclaredField("attributes");
            f.setAccessible(true);
            attributeSetter = lookup.unreflectSetter(f);
            attributeGetter = lookup.unreflectGetter(f);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize helper.", e);
        }
    }

}
