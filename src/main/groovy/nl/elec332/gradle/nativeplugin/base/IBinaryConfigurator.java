package nl.elec332.gradle.nativeplugin.base;

import org.gradle.api.Project;
import org.gradle.api.component.PublishableComponent;
import org.gradle.language.ComponentWithOutputs;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppExecutable;
import org.gradle.language.cpp.CppSharedLibrary;
import org.gradle.language.cpp.CppStaticLibrary;
import org.gradle.language.nativeplatform.ComponentWithExecutable;
import org.gradle.language.nativeplatform.ComponentWithInstallation;
import org.gradle.language.nativeplatform.ComponentWithLinkUsage;
import org.gradle.language.nativeplatform.ComponentWithRuntimeUsage;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;

/**
 * Created by Elec332 on 1/30/2021
 */
@SuppressWarnings("UnstableApiUsage")
public interface IBinaryConfigurator<T> {

    default void configureBinary(Project project, CppBinary binary, T data) {
    }

    default <B extends CppBinary & ComponentWithOutputs & ComponentWithRuntimeUsage & PublishableComponent> void configurePublishableBinary(Project project, B binary, T data) {
    }

    default <B extends CppBinary & ComponentWithExecutable & ComponentWithInstallation> void configureExecutableBinary(Project project, B binary, T data) {
    }

    default void configurePublishableExecutableBinary(Project project, CppExecutable binary, T data) {
    }

    default void configureTestExecutableBinary(Project project, CppTestExecutable binary, T data) {
    }

    default <B extends CppBinary & ComponentWithLinkUsage & ComponentWithOutputs & ComponentWithRuntimeUsage & PublishableComponent> void configureLibraryBinary(Project project, B binary, T data) {
    }

    default void configureSharedLibraryBinary(Project project, CppSharedLibrary binary, T data) {
    }

    default void configureStaticLibraryBinary(Project project, CppStaticLibrary binary, T data) {
    }

}
