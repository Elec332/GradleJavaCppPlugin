package nl.elec332.gradle.nativeplugin;

import org.gradle.api.Project;

/**
 * Created by Elec332 on 4-4-2020
 */
public class DefaultPlatforms {

    private static final String[] DEFAULT_PLATFORMS = new String[]{
            "windows", "linux", "osx"
    };

    public static final String[] DEFAULT_ARCHITECTURES = new String[]{
            "x86", "x64", "amd64", "arm64", "arm", "ppc64"
    };

    @SuppressWarnings("UnstableApiUsage")
    public static void registerDefaultPlatforms(Project project) {
        GroovyHooks.modifyPlatforms(project, platforms -> {
            for (String platform : DEFAULT_PLATFORMS) {
                for (String arch : DEFAULT_ARCHITECTURES) {
                    platforms.register(platform + "_" + arch, p -> {
                        p.architecture(arch);
                        p.operatingSystem(platform);
                    });
                    platforms.register(platform + "-" + arch, p -> {
                        p.architecture(arch);
                        p.operatingSystem(platform);
                    });
                }
            }
        });
    }

}
