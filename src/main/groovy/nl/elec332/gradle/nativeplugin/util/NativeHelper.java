package nl.elec332.gradle.nativeplugin.util;

import nl.elec332.gradle.util.GroovyHooks;
import nl.elec332.gradle.util.Utils;
import org.gradle.api.Project;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.internal.DefaultCppBinary;
import org.gradle.language.cpp.internal.NativeVariantIdentity;
import org.gradle.nativeplatform.toolchain.VisualCpp;

/**
 * Created by Elec332 on 1/30/2021
 */
@SuppressWarnings("UnstableApiUsage")
public class NativeHelper {

    public static void addBuildTools(Project project, String installLocation) {
        if (Utils.isWindows() && !Utils.isNullOrEmpty(installLocation)) {
            GroovyHooks.configureToolChains(project, nativeToolChains -> {
                if (nativeToolChains.isEmpty()) {
                    System.out.println("No toolchains were detected by Gradle, applying VCBT settings...");
                    nativeToolChains.create("visualCppBT", VisualCpp.class, tc ->
                            tc.setInstallDir(installLocation)
                    );
                }
            });
        }
    }

    public static boolean isDebug(CppBinary binary) {
        return binary.isDebuggable() && !binary.isOptimized();
    }

    public static AttributeContainer getAttributes(CppBinary binary) {
        NativeVariantIdentity identity = ((DefaultCppBinary) binary).getIdentity();
        return identity.getRuntimeUsageContext().getAttributes();
    }

}
