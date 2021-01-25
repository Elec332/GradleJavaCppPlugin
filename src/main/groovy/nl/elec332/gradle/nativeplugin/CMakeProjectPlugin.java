package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.nativeplugin.cmake.CMakeHelper;
import nl.elec332.gradle.nativeplugin.cmake.ICMakeSettings;
import nl.elec332.gradle.nativeplugin.common.AbstractCppPlugin;
import nl.elec332.gradle.nativeplugin.common.Constants;
import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.plugins.CppBasePlugin;
import org.gradle.language.swift.plugins.SwiftBasePlugin;
import org.gradle.nativeplatform.toolchain.VisualCpp;
import org.gradle.nativeplatform.toolchain.internal.plugins.StandardToolChainsPlugin;

import static org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE;

/**
 * Created by Elec332 on 1/24/2021
 */
@NonNullApi
@SuppressWarnings("UnstableApiUsage")
public class CMakeProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        project.getPluginManager().apply(StandardToolChainsPlugin.class);
        project.getPluginManager().apply(LifecycleBasePlugin.class);
        project.getPlugins().whenPluginAdded(plugin -> {
            if (plugin instanceof CppBasePlugin || plugin instanceof SwiftBasePlugin) {
                throw new UnsupportedOperationException();
            }
        });

        ICMakeSettings settings = CMakeHelper.newSettingsExtension(project);

        Usage cppApiUsage = project.getObjects().named(Usage.class, Usage.C_PLUS_PLUS_API);
        Usage linkUsage = project.getObjects().named(Usage.class, Usage.NATIVE_LINK);
        Usage runtimeUsage = project.getObjects().named(Usage.class, Usage.NATIVE_RUNTIME);

        Configuration implementation = project.getConfigurations().create("implementation", it -> {
            it.setCanBeConsumed(false);
            it.setCanBeResolved(false);
        });

        Configuration headers = project.getConfigurations().create(AbstractCppPlugin.HEADERS, it -> {
            it.setCanBeResolved(true);
            it.extendsFrom(implementation);
            it.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, cppApiUsage);
            it.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, DIRECTORY_TYPE);
        });

        Configuration linkDebug = project.getConfigurations().create("linkDebug", it -> {
            it.setCanBeResolved(false);
            it.extendsFrom(implementation);
            it.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, linkUsage);
            it.getAttributes().attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, true);
            it.getAttributes().attribute(CppBinary.OPTIMIZED_ATTRIBUTE, false);
        });
        Configuration linkRelease = project.getConfigurations().create("linkRelease", it -> {
            it.setCanBeResolved(false);
            it.extendsFrom(implementation);
            it.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, linkUsage);
            it.getAttributes().attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, true);
            it.getAttributes().attribute(CppBinary.OPTIMIZED_ATTRIBUTE, true);
        });

        Configuration runtimeDebug = project.getConfigurations().create("runtimeDebug", it -> {
            it.setCanBeResolved(false);
            it.extendsFrom(implementation);
            it.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, runtimeUsage);
            it.getAttributes().attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, true);
            it.getAttributes().attribute(CppBinary.OPTIMIZED_ATTRIBUTE, false);
        });
        Configuration runtimeRelease = project.getConfigurations().create("runtimeRelease", it -> {
            it.setCanBeResolved(false);
            it.extendsFrom(implementation);
            it.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, runtimeUsage);
            it.getAttributes().attribute(CppBinary.DEBUGGABLE_ATTRIBUTE, true);
            it.getAttributes().attribute(CppBinary.OPTIMIZED_ATTRIBUTE, true);
        });

        project.getExtensions().getByType(ExtraPropertiesExtension.class).set("buildToolsInstallDir", null);
        project.afterEvaluate(p -> {
            if (Utils.isWindows() && !Utils.isNullOrEmpty((String) project.getProperties().get("buildToolsInstallDir"))) {
                GroovyHooks.configureToolChains(project, nativeToolChains -> {
                    if (nativeToolChains.isEmpty()) {
                        System.out.println("No toolchains were detected by Gradle, applying VCBT settings...");
                        nativeToolChains.create("visualCppBT", VisualCpp.class, tc ->
                                tc.setInstallDir(project.getProperties().get("buildToolsInstallDir"))
                        );
                    }
                });
            }
        });
        CMakeHelper.registerCMakeProject(project, settings, linkRelease, runtimeRelease, linkDebug, runtimeDebug);

        headers.getOutgoing().artifact(settings.getIncludeDirectory());
    }

}
