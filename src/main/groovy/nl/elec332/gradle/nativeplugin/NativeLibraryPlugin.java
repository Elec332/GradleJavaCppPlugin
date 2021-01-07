package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.nativeplugin.common.AbstractNativePlugin;
import org.gradle.language.cpp.plugins.CppLibraryPlugin;

import javax.annotation.Nonnull;

/**
 * Created by Elec332 on 8-11-2020
 */
public class NativeLibraryPlugin extends AbstractNativePlugin {

    @Nonnull
    @Override
    protected Class<?> getPluginType() {
        return CppLibraryPlugin.class;
    }

}
