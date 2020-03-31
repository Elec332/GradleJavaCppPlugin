package nl.elec332.gradle.nativeplugin

import org.gradle.api.Project
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal

import java.util.function.Consumer

/**
 * Created by Elec332 on 31-3-2020
 */
class GroovyHooks {

    static void addModelComponent(Project project, String name, Consumer<NativeLibrarySpec> lib, Consumer<NativeBinarySpecInternal> bins) {
        project.model {
            components {
                "$name"(NativeLibrarySpec) {
                    lib.accept(it)
                    binaries.all {
                        bins.accept(it)
                    }
                }
            }
        }
    }

}
