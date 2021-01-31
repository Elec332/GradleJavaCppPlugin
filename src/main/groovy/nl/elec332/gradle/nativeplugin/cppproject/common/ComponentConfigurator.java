package nl.elec332.gradle.nativeplugin.cppproject.common;

import nl.elec332.gradle.nativeplugin.base.IComponentConfigurator;
import nl.elec332.gradle.nativeplugin.cppproject.extensions.NativeProjectExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.CppLibrary;

import java.util.function.Consumer;

/**
 * Created by Elec332 on 6-1-2021
 */
public class ComponentConfigurator implements IComponentConfigurator<NativeProjectExtension> {

    @Override
    public void configureComponent(Project project, CppComponent component, Consumer<Runnable> callbacks, NativeProjectExtension data) {
        component.getPrivateHeaders().from("src/" + component.getName() + "/headers");
        component.getPrivateHeaders().from(data.getGeneratedHeadersDir());
    }

    @Override
    public void configureLibrary(Project project, CppLibrary component, Consumer<Runnable> callbacks, NativeProjectExtension data) {
        component.getLinkage().set(data.getLinkage());
        ((org.gradle.language.cpp.internal.DefaultCppLibrary) component).getApiElements().getOutgoing().artifact(data.getGeneratedHeadersDir());
        project.getTasks().configureEach(task -> {
            if (task instanceof Zip && task.getName().equals("cppHeaders")) {
                ((Zip) task).from(data.getGeneratedHeadersDir());
            }
        });
    }

}
