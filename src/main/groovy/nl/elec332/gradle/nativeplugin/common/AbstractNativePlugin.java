package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.nativeplugin.api.INativeProjectExtension;
import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.*;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.language.ComponentWithBinaries;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppExecutable;
import org.gradle.language.cpp.CppSharedLibrary;
import org.gradle.language.cpp.CppStaticLibrary;
import org.gradle.language.cpp.internal.DefaultCppLibrary;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.language.internal.NativeComponentFactory;
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithLinkUsage;
import org.gradle.language.nativeplatform.internal.toolchains.ToolChainSelector;
import org.gradle.nativeplatform.Linkage;
import org.gradle.nativeplatform.TargetMachineFactory;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.internal.OperatingSystemInternal;
import org.gradle.nativeplatform.toolchain.VisualCpp;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 6-11-2020
 */
@NonNullApi
public abstract class AbstractNativePlugin implements Plugin<Project> {

    public AbstractNativePlugin(boolean isLibrary) {
        this.isLibrary = isLibrary;
    }

    public static final String HEADERS = "headers";
    public static final String WINDOWS_HEADERS = "windowsHeaders";
    public static final String STATIC_LINKER = "staticLinker";
    public static final String LINKER = "linker";
    public static final String DYNAMIC = "dynamic";

    private final boolean isLibrary;
    private static final OperatingSystem os = OperatingSystem.current();

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion("6.0");
        NativeProjectExtension nativeProject = (NativeProjectExtension) project.getExtensions().create(INativeProjectExtension.class, "nativeProject", NativeProjectExtension.class, project);

        project.getConfigurations().create(HEADERS);
        project.getConfigurations().create(WINDOWS_HEADERS);
        project.getConfigurations().create(STATIC_LINKER);
        project.getConfigurations().create(LINKER);
        project.getConfigurations().create(DYNAMIC);

        File generatedHeaders = new File(ProjectHelper.getBuildFolder(project), "/tmp/" + project.getName().trim().toLowerCase(Locale.ROOT) + "GeneratedHeaders");
        project.getExtensions().add("includedDeps", "");
        project.getExtensions().add("excludedDeps", "");
        System.out.println(generatedHeaders);

        //Set C++ version
        nativeProject.modifyCompiler(compiler -> {
            compiler.getCompilerArgs().addAll(compiler.getToolChain().map(toolChain -> {
                if (toolChain instanceof VisualCpp) {
                    return Collections.singletonList("/std:" + nativeProject.getCppVersion().get());
                } else {
                    return Collections.singletonList("-std=" + nativeProject.getCppVersion().get());
                }
            }));
        });

        //Generate include header
        nativeProject.modifyCompiler(compiler -> {
            HeaderGenerator.generateHeaders(project, generatedHeaders, nativeProject);
            compiler.includes(generatedHeaders);
        });

        project.afterEvaluate(p -> {

            //Set VC BuildTools install dir
            if (os.isWindows() && !Utils.isNullOrEmpty(nativeProject.getBuildToolsInstallDir().get())) {
                GroovyHooks.configureToolchains(project, nativeToolChains ->
                        nativeToolChains.create("visualCppBT", VisualCpp.class, tc ->
                                tc.setInstallDir(nativeProject.getBuildToolsInstallDir().get())
                        )
                );
            }

            //Set linkage and add generated headers to output products
            if (isLibrary) {
                DefaultCppLibrary libSettings = (DefaultCppLibrary) project.getExtensions().getByName("library");
                libSettings.getLinkage().set(nativeProject.getLinkage());
                libSettings.getApiElements().getOutgoing().artifact(generatedHeaders);
            }

            //Run binary modifiers
            modifyBinaries(project, binary -> {
                if (isLibrary) {
                    Linkage linkage = ((ConfigurableComponentWithLinkUsage) binary).getLinkage();
                    if (linkage == Linkage.STATIC) {
                        CppStaticLibrary lib = (CppStaticLibrary) binary;
                        BinaryConfigurator.configureStaticLibraryBinary(project, nativeProject, lib);
                        BinaryConfigurator.configureLibraryBinary(project, nativeProject, lib);
                        BinaryConfigurator.configureBinary(project, nativeProject, lib);
                    } else {
                        CppSharedLibrary lib = (CppSharedLibrary) binary;
                        BinaryConfigurator.configureSharedLibraryBinary(project, nativeProject, lib);
                        BinaryConfigurator.configureLibraryBinary(project, nativeProject, lib);
                        BinaryConfigurator.configureBinary(project, nativeProject, lib);
                    }
                } else {
                    CppExecutable executable = (CppExecutable) binary;
                    BinaryConfigurator.configureExecutableBinary(project, nativeProject, executable);
                    BinaryConfigurator.configureBinary(project, nativeProject, executable);
                }
            });

            //Run compiler modifiers
            project.getTasks().withType(CppCompile.class).configureEach(c -> nativeProject.getCompilerMods().forEach(a -> a.execute(c)));

        });

        //Apply gradle native plugin
        project.getPluginManager().apply(getPluginType());
    }

    private void modifyBinaries(Project project, Consumer<CppBinary> consumer) {
        SoftwareComponent component = project.getComponents().getAt("main");
        if (component instanceof ComponentWithBinaries) {
            ((ComponentWithBinaries) component).getBinaries().whenElementFinalized(softwareComponent -> {
                if (softwareComponent instanceof CppBinary) {
                    CppBinary binary = (CppBinary) softwareComponent;
                    consumer.accept(binary);
                }
            });
        }
    }

    public static org.gradle.internal.os.OperatingSystem getOperatingSystemInfo(NativePlatform platform) {
        return ((OperatingSystemInternal) platform.getOperatingSystem()).getInternalOs();
    }

    protected abstract Class<?> getPluginType();

}
