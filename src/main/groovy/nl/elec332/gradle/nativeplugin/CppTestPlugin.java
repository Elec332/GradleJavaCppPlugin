package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.nativeplugin.api.INativeProjectExtension;
import nl.elec332.gradle.nativeplugin.common.Constants;
import nl.elec332.gradle.util.PluginHelper;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;
import org.gradle.nativeplatform.test.cpp.plugins.CppUnitTestPlugin;

import java.util.Collections;

/**
 * Created by Elec332 on 1/19/2021
 */
@NonNullApi
public class CppTestPlugin implements Plugin<Project> {

    @Override
    @SuppressWarnings("all")
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        project.getPluginManager().apply(CppUnitTestPlugin.class);
        project.afterEvaluate(p -> {
            INativeProjectExtension extension = project.getExtensions().findByType(INativeProjectExtension.class);
            if (extension != null) {
                project.getExtensions().getByType(CppTestSuite.class).getTestBinary().flatMap(CppTestExecutable::getRunTask).get().args(extension.getTestArguments().getOrElse(Collections.emptySet()));
            }
        });
    }

}