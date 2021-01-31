package nl.elec332.gradle.nativeplugin.cppproject.common;

import nl.elec332.gradle.nativeplugin.api.cppproject.INativeProjectExtension;
import nl.elec332.gradle.nativeplugin.base.CppUtilsPlugin;
import nl.elec332.gradle.nativeplugin.cppproject.extensions.NativeProjectExtension;
import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.nativeplugin.util.NativeHelper;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.nativeplatform.toolchain.VisualCpp;

import java.io.File;
import java.util.Collections;
import java.util.Locale;

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

        project.getPlugins().apply(CppUtilsPlugin.class);
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

        project.afterEvaluate(p -> {

            project.afterEvaluate(p2 -> {

                //Generate include header
                GeneratedHeaderHandler.generateHeaders(generatedHeaders, nativeProject);

            });

            //Add custom BuildTools if defined
            NativeHelper.addBuildTools(project, nativeProject.getBuildToolsInstallDir().get());

        });

        //Apply gradle native plugin
        project.getPluginManager().apply(getPluginType());
    }

    protected abstract Class<?> getPluginType();

}
