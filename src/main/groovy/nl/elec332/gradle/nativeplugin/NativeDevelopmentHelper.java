package nl.elec332.gradle.nativeplugin;

import org.gradle.language.cpp.CppSourceSet;
import org.gradle.nativeplatform.*;
import org.gradle.nativeplatform.platform.NativePlatform;
import org.gradle.nativeplatform.platform.OperatingSystem;
import org.gradle.nativeplatform.platform.internal.ArchitectureInternal;
import org.gradle.nativeplatform.platform.internal.OperatingSystemInternal;
import org.gradle.nativeplatform.test.NativeTestSuiteBinarySpec;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;

/**
 * Created by Elec332 on 31-3-2020
 */
public class NativeDevelopmentHelper {

    @Nonnull
    @SuppressWarnings("UnstableApiUsage")
    public static File getLibraryOutput(NativeBinarySpec binSpec) {
        if (binSpec instanceof SharedLibraryBinarySpec) {
            return ((SharedLibraryBinarySpec) binSpec).getSharedLibraryFile();
        } else if (binSpec instanceof StaticLibraryBinarySpec) {
            return ((StaticLibraryBinarySpec) binSpec).getStaticLibraryFile();
        } else if (binSpec instanceof NativeTestSuiteBinarySpec) {
            return ((NativeTestSuiteBinarySpec) binSpec).getExecutableFile();
        } else if (binSpec instanceof NativeExecutableBinarySpec) {
            return ((NativeExecutableBinarySpec) binSpec).getExecutable().getFile();
        }
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @SuppressWarnings("UnstableApiUsage")
    public static CppSourceSet getCppSourceSet(NativeLibrarySpec spec) {
        return Objects.requireNonNull((CppSourceSet) spec.getSources().get("cpp"));
    }

    @SuppressWarnings("UnstableApiUsage")
    public static String getLibName(NativePlatform targetPlatform) {
        return getOperatingSystemInfo(targetPlatform).getLinkLibrarySuffix();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static org.gradle.internal.os.OperatingSystem getOperatingSystemInfo(NativePlatform platform) {
        return ((OperatingSystemInternal) platform.getOperatingSystem()).getInternalOs();
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
        } else if (os.isMacOsX()) {
            osName = "macosx";
        } else if (os.isFreeBSD() || os.isSolaris()) {
            throw new UnsupportedOperationException();
        }

        if (platform.isAmd64()) {
            archName = "x86_64";
        } else if (platform.isArm()) {
            archName = "arm";
        } else if (platform.isI386()) {
            archName = "x86";
        } else if (platform.isIa64()) { //Get out
            throw new UnsupportedOperationException();
        } else {
            if (platform.getName().contains("arm-")) {
                archName = "arm64";
            } else if (platform.getName().contains("ppc64")) {
                archName = "ppc64le";
            } else {
                throw new UnsupportedOperationException();
            }
        }

        return osName + "-" + archName;
    }

}
