package nl.elec332.gradle.nativeplugin.jetbrains;

import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.util.PluginHelper;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.language.ComponentWithBinaries;
import org.gradle.language.cpp.plugins.CppBasePlugin;
import org.gradle.language.nativeplatform.ComponentWithExecutable;
import org.gradle.language.nativeplatform.ComponentWithInstallation;
import org.gradle.nativeplatform.tasks.InstallExecutable;
import org.gradle.nativeplatform.tasks.LinkExecutable;

/**
 * Created by Elec332 on 2/6/2021
 */
@NonNullApi
public class CLionRunConfigPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        project.getPlugins().whenPluginAdded(p -> {
            if (p instanceof CppBasePlugin) {
                project.afterEvaluate(this::configure);
            }
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private void configure(Project project) {
        project.getComponents().forEach(component -> {
            if (component instanceof ComponentWithBinaries) {
                ((ComponentWithBinaries) component).getBinaries().whenElementFinalized(softwareComponent -> {
                    if (softwareComponent instanceof ComponentWithExecutable && softwareComponent instanceof ComponentWithInstallation) {
                        InstallExecutable installTask = ((ComponentWithInstallation) softwareComponent).getInstallTask().get();
                        LinkExecutable linkTask = ((ComponentWithExecutable) softwareComponent).getLinkTask().get();
                        installTask.getExecutableFile().set(linkTask.getLinkedFile());
                        ((RegularFileProperty) ((ComponentWithExecutable) softwareComponent).getExecutableFile()).set(installTask.getRunScriptFile().get());
                        linkTask.finalizedBy(installTask);
                    }
                });
            }
        });
    }

}
