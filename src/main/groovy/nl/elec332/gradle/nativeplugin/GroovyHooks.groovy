package nl.elec332.gradle.nativeplugin

import nl.elec332.gradle.util.ProjectHelper
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.internal.NativeBinarySpecInternal
import org.gradle.nativeplatform.platform.NativePlatform

import java.util.function.Consumer

/**
 * Created by Elec332 on 31-3-2020
 */
class GroovyHooks {

    static void addModelComponent(Project project, String name, Consumer<NativeLibrarySpec> lib, Consumer<NativeBinarySpecInternal> bin) {
        Set<NativeBinarySpecInternal> stuff = new HashSet<>();
        project.model {
            components {
                "$name"(NativeLibrarySpec) {
                    lib.accept(it)
                    binaries.all {
                        stuff.add(it)
                    }
                }
            }
        }
        ProjectHelper.afterNativeModelExamined(project, { ->
            for (NativeBinarySpecInternal bs : stuff) {
                bin.accept(bs)
            }
        })
    }

    static void modifyPlatforms(Project project, Consumer<ExtensiblePolymorphicDomainObjectContainer<NativePlatform>> modifier) {
        project.model {
            platforms {
                modifier.accept(it)
                it.toArray() //Force it to process new entries
            }
        }
    }

}
