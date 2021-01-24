package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.nativeplugin.api.INativeProjectExtension;
import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Attribute;
import org.gradle.language.ComponentWithBinaries;
import org.gradle.language.cpp.*;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;
import org.gradle.nativeplatform.toolchain.VisualCpp;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 6-11-2020
 */
@NonNullApi
public abstract class AbstractCppPlugin implements Plugin<Project> {

    public static final String HEADERS = "headers";
    public static final String WINDOWS_HEADERS = "windowsHeaders";
    public static final String STATIC_LINKER = "staticLinker";

    public static final String LINKER = "linker";
    public static final String DYNAMIC = "dynamic";

    public static final Attribute<Boolean> SMALL_ATTRIBUTE = Attribute.of("small", Boolean.class);

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        File generatedHeaders = new File(ProjectHelper.getBuildFolder(project), "/tmp/" + project.getName().trim().toLowerCase(Locale.ROOT) + "GeneratedHeaders");
        NativeProjectExtension nativeProject = (NativeProjectExtension) project.getExtensions().create(INativeProjectExtension.class, "nativeProject", NativeProjectExtension.class, project, generatedHeaders);

        project.getConfigurations().create(HEADERS);
        project.getConfigurations().create(WINDOWS_HEADERS);
        project.getConfigurations().create(STATIC_LINKER);
        project.getConfigurations().create(LINKER);
        project.getConfigurations().create(DYNAMIC);

        project.getDependencies().getAttributesSchema().attribute(SMALL_ATTRIBUTE);

        project.getExtensions().add("includedDeps", "");
        project.getExtensions().add("excludedDeps", "");

        InternalHelper helper = project.getObjects().newInstance(InternalHelper.class);

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
        nativeProject.modifyCompiler(compiler -> {
            compiler.includes(generatedHeaders);
            compiler.getCompilerArgs().add("-D" + nativeProject.getGeneratedHeaderSubFolder().get().toUpperCase(Locale.ROOT) + "_CREATE_EXPORTS");
        });

        Set<Runnable> callbacks = new HashSet<>();
        project.afterEvaluate(p -> {

            //Generate include header
            HeaderGenerator.generateHeaders(generatedHeaders, nativeProject);

            if (Utils.isWindows() && !Utils.isNullOrEmpty(nativeProject.getBuildToolsInstallDir().get())) {
                GroovyHooks.configureToolChains(project, nativeToolChains -> {
                    if (nativeToolChains.isEmpty()) {
                        System.out.println("No toolchains were detected by Gradle, applying VCBT settings...");
                        nativeToolChains.create("visualCppBT", VisualCpp.class, tc ->
                                tc.setInstallDir(nativeProject.getBuildToolsInstallDir().get())
                        );
                    }
                });
            }

            project.getComponents().forEach(softwareComponent -> {
                if (softwareComponent instanceof CppComponent) {
                    ComponentConfigurator.configureComponent(project, nativeProject, (CppComponent) softwareComponent, callbacks::add);
                    if (softwareComponent instanceof CppLibrary) {
                        ComponentConfigurator.configureLibrary(project, helper, nativeProject, (CppLibrary) softwareComponent, callbacks::add);
                    } else if (softwareComponent instanceof CppApplication) {
                        ComponentConfigurator.configureExecutable(project, helper, nativeProject, (CppApplication) softwareComponent, callbacks::add);
                    } // No need to alter CppTestSuite (yet)
                }
            });

            //Run binary modifiers
            modifyBinaries(project, binary -> {
                BinaryConfigurator.configureBinary(project, helper, nativeProject, binary);
                if (binary instanceof CppStaticLibrary) {
                    CppStaticLibrary lib = (CppStaticLibrary) binary;
                    BinaryConfigurator.configureStaticLibraryBinary(project, helper, nativeProject, lib);
                    BinaryConfigurator.configureLibraryBinary(project, helper, nativeProject, lib);
                    BinaryConfigurator.configurePublishableBinary(project, helper, nativeProject, lib);
                } else if (binary instanceof CppSharedLibrary){
                    CppSharedLibrary lib = (CppSharedLibrary) binary;
                    BinaryConfigurator.configureSharedLibraryBinary(project, helper, nativeProject, lib);
                    BinaryConfigurator.configureLibraryBinary(project, helper, nativeProject, lib);
                    BinaryConfigurator.configurePublishableBinary(project, helper, nativeProject, lib);
                } else if (binary instanceof CppExecutable) {
                    CppExecutable executable = (CppExecutable) binary;
                    BinaryConfigurator.configurePublishableExecutableBinary(project, helper, nativeProject, executable);
                    BinaryConfigurator.configureExecutableBinary(project, helper, nativeProject, executable);
                    BinaryConfigurator.configurePublishableBinary(project, helper, nativeProject, executable);
                } else if (binary instanceof CppTestExecutable){
                    CppTestExecutable executable = (CppTestExecutable) binary;
                    BinaryConfigurator.configureTestExecutableBinary(project, helper, nativeProject, executable);
                    BinaryConfigurator.configureExecutableBinary(project, helper, nativeProject, executable);
                } else {
                    throw new UnsupportedOperationException("Unknown library type: " + binary.getClass());
                }
            });

            //Run compiler modifiers
            project.getTasks().withType(CppCompile.class).configureEach(c -> nativeProject.getCompilerMods().forEach(a -> a.execute(c)));

        });

        //Apply gradle native plugin
        project.getPluginManager().apply(getPluginType());

        project.afterEvaluate(p -> callbacks.forEach(Runnable::run));
    }

    private void modifyBinaries(Project project, Consumer<CppBinary> consumer) {
        project.getComponents().forEach(component -> {
            if (component instanceof ComponentWithBinaries) {
                ((ComponentWithBinaries) component).getBinaries().whenElementFinalized(softwareComponent -> {
                    if (softwareComponent instanceof CppBinary) {
                        CppBinary binary = (CppBinary) softwareComponent;
                        consumer.accept(binary);
                    }
                });
            }
        });
    }

    protected abstract Class<?> getPluginType();

}
