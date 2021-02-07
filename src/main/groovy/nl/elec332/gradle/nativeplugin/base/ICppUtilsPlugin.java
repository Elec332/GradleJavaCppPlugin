package nl.elec332.gradle.nativeplugin.base;

import org.gradle.api.Action;
import org.gradle.language.cpp.tasks.CppCompile;

/**
 * Created by Elec332 on 1/30/2021
 */
public interface ICppUtilsPlugin {

    <T> void addComponentConfigurator(IComponentConfigurator<T> configurator, T data);

    <T> void modifyComponentsDirect(IComponentConfigurator<T> configurator, T data);

    <T> void addBinaryConfigurator(IBinaryConfigurator<T> configurator, T data);

    void modifyCompiler(Action<? super CppCompile> action);

}
