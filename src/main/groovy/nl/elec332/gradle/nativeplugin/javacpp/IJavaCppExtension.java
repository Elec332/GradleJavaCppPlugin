package nl.elec332.gradle.nativeplugin.javacpp;

import org.gradle.api.provider.Property;

/**
 * Created by Elec332 on 2/20/2021
 */
public interface IJavaCppExtension {

    Property<String> getDefaultJavaCPPVersion();

    Property<String> getMainClass();

}
