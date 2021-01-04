package nl.elec332.gradle.nativeplugin;

import nl.elec332.gradle.nativeplugin.common.AbstractNativePlugin;
import org.gradle.language.cpp.plugins.CppApplicationPlugin;

import javax.annotation.Nonnull;

/**
 * Created by Elec332 on 7-11-2020
 */
public class NativeApplicationPlugin extends AbstractNativePlugin {

    public NativeApplicationPlugin() {
        super(false);
    }

    @Nonnull
    @Override
    protected Class<?> getPluginType() {
        return CppApplicationPlugin.class;
    }

}
