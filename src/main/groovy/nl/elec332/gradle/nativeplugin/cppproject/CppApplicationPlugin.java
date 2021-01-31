package nl.elec332.gradle.nativeplugin.cppproject;

import nl.elec332.gradle.nativeplugin.cppproject.common.AbstractCppPlugin;

import javax.annotation.Nonnull;

/**
 * Created by Elec332 on 7-11-2020
 */
public class CppApplicationPlugin extends AbstractCppPlugin {

    @Nonnull
    @Override
    protected Class<?> getPluginType() {
        return org.gradle.language.cpp.plugins.CppApplicationPlugin.class;
    }

}
