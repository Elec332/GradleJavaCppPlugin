package nl.elec332.gradle.nativeplugin.base;

import org.gradle.api.Action;
import org.gradle.language.cpp.CppBinary;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.tasks.LinkExecutable;

/**
 * Created by Elec332 on 1/30/2021
 */
public interface ICppUtilsPlugin {

    <T> void addComponentConfigurator(IComponentConfigurator<T> configurator, T data);

    <T> void addBinaryConfigurator(IBinaryConfigurator<T> configurator, T data);

    void modifyCompilers(Action<? super CppCompile> action);

    void modifyLinkers(Action<? super LinkExecutable> action);

    void modifyComponents(Action<? super CppComponent> action);

    <T> void modifyComponentsDirect(IComponentConfigurator<T> configurator, T data);

    void modifyBinaries(Action<? super CppBinary> action);

}
