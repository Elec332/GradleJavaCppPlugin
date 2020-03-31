package nl.elec332.gradle.nativeplugin;

import org.gradle.language.cpp.CppSourceSet;
import org.gradle.nativeplatform.NativeComponentSpec;
import org.gradle.nativeplatform.NativeLibrarySpec;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.OperatingSystem;
import org.gradle.nativeplatform.platform.internal.ArchitectureInternal;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Created by Elec332 on 31-3-2020
 */
public class NativeDevelopmentHelper {

    @Nonnull
    @SuppressWarnings("UnstableApiUsage")
    public static CppSourceSet getCppSourceSet(NativeLibrarySpec spec) {
        return Objects.requireNonNull((CppSourceSet) spec.getSources().get("cpp"));
    }

    @SuppressWarnings("UnstableApiUsage")
    public static String getLibName(NativePlatform targetPlatform) {
        if (targetPlatform.getOperatingSystem().isWindows()) {
            return ".lib";
        }
        return ".so";
    }

    @SuppressWarnings("all")
    public static String getFolderName(NativePlatform targetPlatform) {
        OperatingSystem os = targetPlatform.getOperatingSystem();
        ArchitectureInternal platform = (ArchitectureInternal) targetPlatform.getArchitecture();

        String osName = "<error>";
        String archName = "<error>";
        if (os.isWindows()) {
            osName = "windows";
        } else if (os.isLinux()) {
            osName = "linux";
        } else if (os.isSolaris()) {
            throw new UnsupportedOperationException();
        } else if (os.isMacOsX()) {
            osName = "macosx";
        } else if (os.isFreeBSD()) {
            throw new UnsupportedOperationException();
        }

        if (platform.isAmd64()) {
            archName = "x86_64";
        } else if (platform.isArm()) {
            archName = "arm64";
        } else if (platform.isI386()) {
            archName = "x86";
        } else if (platform.isIa64()) { //Get out
            throw new UnsupportedOperationException();
        }

        return osName + "-" + archName;
    }

}
