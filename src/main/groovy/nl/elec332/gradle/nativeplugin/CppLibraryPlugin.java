package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.nativeplugin.common.AbstractCppPlugin;

import javax.annotation.Nonnull;

/**
 * Created by Elec332 on 8-11-2020
 */
public class CppLibraryPlugin extends AbstractCppPlugin {

    @Nonnull
    @Override
    protected Class<?> getPluginType() {
        return org.gradle.language.cpp.plugins.CppLibraryPlugin.class;
    }

}
