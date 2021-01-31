package nl.elec332.gradle.nativeplugin.base;

import org.gradle.api.Project;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.CppLibrary;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 1/30/2021
 */
public interface IComponentConfigurator<T> {

    default void configureComponent(Project project, CppComponent component, Consumer<Runnable> callbacks, T data) {
    }

    default void configureLibrary(Project project, CppLibrary component, Consumer<Runnable> callbacks, T data) {
    }

    default void configureExecutable(Project project, CppApplication component, Consumer<Runnable> callbacks, T data) {
    }

}
