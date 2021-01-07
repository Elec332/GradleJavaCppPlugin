package nl.elec332.gradle.nativeplugin.common;

import org.gradle.api.Project;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppLibrary;
import org.gradle.language.cpp.ProductionCppComponent;

/**
 * Created by Elec332 on 6-1-2021
 */
public class ComponentConfigurator {

    static void configureComponent(Project project, NativeProjectExtension nativeProject, ProductionCppComponent component) {

    }

    static void configureLibrary(Project project, NativeProjectExtension nativeProject, CppLibrary component) {
        component.getLinkage().set(nativeProject.getLinkage());
        ((org.gradle.language.cpp.internal.DefaultCppLibrary) component).getApiElements().getOutgoing().artifact(nativeProject.getGeneratedHeadersDir());
    }

    static void configureExecutable(Project project, NativeProjectExtension nativeProject, CppApplication component) {

    }

    /////////////////////////////////////////////////////////////////

}
