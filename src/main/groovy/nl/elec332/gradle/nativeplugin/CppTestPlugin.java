package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.nativeplugin.common.Constants;
import nl.elec332.gradle.util.PluginHelper;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.nativeplatform.test.cpp.plugins.CppUnitTestPlugin;

/**
 * Created by Elec332 on 1/19/2021
 */
@NonNullApi
public class CppTestPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        project.getPluginManager().apply(CppUnitTestPlugin.class);
    }

}