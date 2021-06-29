package nl.elec332.gradle.nativeplugin.cppproject.common;

import nl.elec332.gradle.nativeplugin.api.cppproject.INativeProjectExtension;
import nl.elec332.gradle.nativeplugin.base.CppUtilsPlugin;
import nl.elec332.gradle.nativeplugin.base.IComponentConfigurator;
import nl.elec332.gradle.nativeplugin.cppproject.extensions.NativeProjectExtension;
import nl.elec332.gradle.nativeplugin.jetbrains.CLionRunConfigPlugin;
import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.nativeplugin.util.NativeHelper;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppLibrary;
import org.gradle.nativeplatform.toolchain.VisualCpp;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 6-11-2020
 */
@NonNullApi
public abstract class AbstractCppPlugin implements Plugin<Project> {

    public static final String STATIC_LINKER = "staticLinker";

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        File generatedHeaders = new File(ProjectHelper.getBuildFolder(project), "/tmp/" + project.getName().trim().toLowerCase(Locale.ROOT) + "GeneratedHeaders");
        NativeProjectExtension nativeProject = (NativeProjectExtension) project.getExtensions().create(INativeProjectExtension.class, "nativeProject", NativeProjectExtension.class, project, generatedHeaders);

        project.afterEvaluate(p -> {

            //Add custom BuildTools if defined
            NativeHelper.addBuildTools(project, nativeProject.getBuildToolsInstallDir().get());
        });

        project.getPlugins().apply(CppUtilsPlugin.class);
        project.getPlugins().apply(CLionRunConfigPlugin.class);
        project.getConfigurations().create(STATIC_LINKER);

        project.getExtensions().add("includedDeps", "");
        project.getExtensions().add("excludedDeps", "");

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

        CppUtilsPlugin.getBasePlugin(project).addBinaryConfigurator(new BinaryConfigurator(), nativeProject);
        CppUtilsPlugin.getBasePlugin(project).addComponentConfigurator(new ComponentConfigurator(), nativeProject);

        CppUtilsPlugin.getBasePlugin(project).addComponentConfigurator(new IComponentConfigurator<Object>() {

            @Override
            public void configureLibrary(Project project, CppLibrary component, Consumer<Runnable> callbacks, Object data) {
                if (!nativeProject.getSingleRuntimeType().get() && component.getTargetMachines().get().stream().anyMatch(targetMachine -> targetMachine.getOperatingSystemFamily().isWindows())) {
                    if (nativeProject.getStaticRuntime().getOrElse(false)) {
                        VariantConfigurator.addSharedRuntimeVariant(project, component, callbacks);
                    } else {
                        VariantConfigurator.addStaticRuntimeVariant(project, component, callbacks);
                    }
                    TestIntegration.fixTestExecutable(project, component);
                }
            }

            @Override
            public void configureExecutable(Project project, CppApplication component, Consumer<Runnable> callbacks, Object data) {
                if (!nativeProject.getSingleRuntimeType().get() && component.getTargetMachines().get().stream().anyMatch(targetMachine -> targetMachine.getOperatingSystemFamily().isWindows())) {
                    if (nativeProject.getStaticRuntime().getOrElse(false)) {
                        VariantConfigurator.addSharedRuntimeVariant(project, component, callbacks);
                    } else {
                        VariantConfigurator.addStaticRuntimeVariant(project, component, callbacks);
                    }
                    TestIntegration.fixTestExecutable(project, component);
                }
            }

        }, null);

        //Apply gradle native plugin
        project.getPluginManager().apply(getPluginType());

        project.afterEvaluate(p -> {

            //Generate include header
            GeneratedHeaderHandler.generateHeaders(generatedHeaders, nativeProject);

        });

    }

    protected abstract Class<?> getPluginType();

}
