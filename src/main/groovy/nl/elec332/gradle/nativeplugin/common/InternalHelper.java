package nl.elec332.gradle.nativeplugin.common;

import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.component.UsageContext;
import org.gradle.api.plugins.internal.AbstractUsageContext;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.language.cpp.*;
import org.gradle.language.cpp.internal.*;
import org.gradle.language.nativeplatform.internal.Dimensions;
import org.gradle.language.nativeplatform.internal.toolchains.ToolChainSelector;
import org.gradle.nativeplatform.Linkage;

import javax.inject.Inject;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Elec332 on 1/10/2021
 *
 * All dirty hacks and usage of internal Gradle API's can be found here...
 */
public class InternalHelper {

    @Inject
    public InternalHelper(ToolChainSelector toolChainSelector, ImmutableAttributesFactory attributesFactory) {
        this.toolChainSelector = toolChainSelector;
        this.attributesFactory = attributesFactory;
    }

    private final ToolChainSelector toolChainSelector;
    private final ImmutableAttributesFactory attributesFactory;

    public void addLibraryVariant(String name_, String fileSuffix, Project project, CppLibrary component, Consumer<CppBinary> binaryConsumer) {
        DefaultCppLibrary library = (DefaultCppLibrary) component;
        Provider<String> baseName = suffix(project, library.getBaseName(), fileSuffix);
        String name = "_" + name_.toLowerCase(Locale.ROOT);
        Dimensions.libraryVariants(baseName, library.getLinkage(), library.getTargetMachines(), project.getObjects(), attributesFactory,
                project.getProviders().provider(() -> project.getGroup().toString()), project.getProviders().provider(() -> project.getVersion().toString()),
                variantIdentity -> {
                    if (Dimensions.tryToBuildOnHost(variantIdentity)) {
                        ToolChainSelector.Result<CppPlatform> result = toolChainSelector.select(CppPlatform.class, new DefaultCppPlatform(variantIdentity.getTargetMachine()));

                        DefaultCppBinary binary;
                        //System.out.println(variantIdentity.getLinkUsageContext().getName());
                        setName(variantIdentity.getLinkUsageContext(), s -> s + name);
                        setName(variantIdentity.getRuntimeUsageContext(), s -> s + name);
                        //modifyAttributes(variantIdentity.getLinkUsageContext(), AbstractNativePlugin.SMALL_ATTRIBUTE, true);
                        //modifyAttributes(variantIdentity.getRuntimeUsageContext(), AbstractNativePlugin.SMALL_ATTRIBUTE, true);
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

    public void addExecutableVariant(String name_, String fileSuffix, Project project, CppApplication component, Consumer<CppExecutable> binaryConsumer) {
        DefaultCppApplication application = (DefaultCppApplication) component;
        Provider<String> baseName = suffix(project, application.getBaseName(), fileSuffix);
        String name = "_" + name_.toLowerCase(Locale.ROOT);
        Dimensions.applicationVariants(baseName, application.getTargetMachines(), project.getObjects(), attributesFactory,
                project.getProviders().provider(() -> project.getGroup().toString()), project.getProviders().provider(() -> project.getVersion().toString()),
                variantIdentity -> {
                    if (Dimensions.tryToBuildOnHost(variantIdentity)) {
                        ToolChainSelector.Result<CppPlatform> result = toolChainSelector.select(CppPlatform.class, new DefaultCppPlatform(variantIdentity.getTargetMachine()));
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

    public <T> void modifyAttributes(CppBinary binary, Attribute<T> key, T value) {
        NativeVariantIdentity identity = ((DefaultCppBinary) binary).getIdentity();
        if (!identity.getRuntimeUsageContext().getAttributes().contains(key)) {
            modifyAttributes(identity.getRuntimeUsageContext(), key, value);
            if (identity.getLinkUsageContext() != null) {
                modifyAttributes(identity.getLinkUsageContext(), key, value);
            }
        }
    }

    ////////////////////////////////////////////////////

    public void setName(UsageContext context, Function<String, String> modifier) {
        try {
            nameSetter.invoke(context, modifier.apply((String) nameGetter.invoke(context)));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void modifyAttributes(UsageContext context, Attribute<T> key, T value) {
        try {
            attributeSetter.invoke(context, attributesFactory.concat((ImmutableAttributes) attributeGetter.invoke(context), key, value));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle attributeGetter, attributeSetter, nameGetter, nameSetter;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Field f = AbstractUsageContext.class.getDeclaredField("attributes");
            f.setAccessible(true);
            attributeSetter = lookup.unreflectSetter(f);
            attributeGetter = lookup.unreflectGetter(f);
            f = DefaultUsageContext.class.getDeclaredField("name");
            f.setAccessible(true);
            nameSetter = lookup.unreflectSetter(f);
            nameGetter = lookup.unreflectGetter(f);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize helper.", e);
        }
    }

}
