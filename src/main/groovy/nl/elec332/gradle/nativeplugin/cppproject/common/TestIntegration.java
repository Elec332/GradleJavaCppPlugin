package nl.elec332.gradle.nativeplugin.cppproject.common;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.AttributesSchemaInternal;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.internal.DefaultCppBinary;
import org.gradle.language.nativeplatform.tasks.UnexportMainSymbol;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;
import org.gradle.nativeplatform.test.cpp.internal.DefaultCppTestExecutable;
import org.gradle.nativeplatform.test.cpp.internal.DefaultCppTestSuite;
import org.gradle.nativeplatform.test.cpp.plugins.CppUnitTestPlugin;

/**
 * Created by Elec332 on 2/9/2021
 */
public class TestIntegration {

    @SuppressWarnings("all")
    public static void fixTestExecutable(Project project, CppComponent component) { //TODO: Restore my eyesight (again)
        if (!project.getPlugins().hasPlugin(CppUnitTestPlugin.class)) {
            return;
        }
        DefaultCppTestSuite testSuite = (DefaultCppTestSuite) project.getExtensions().getByType(CppTestSuite.class);
        testSuite.getTestedComponent().convention((CppComponent) null);
        testSuite.getBinaries().whenElementKnown(DefaultCppTestExecutable.class, (binary) -> {
            binary.getImplementationDependencies().extendsFrom(component.getImplementationDependencies());
        });
        testSuite.getBinaries().whenElementFinalized(DefaultCppTestExecutable.class, (binary) -> {
            component.getBinaries().whenElementFinalized(testedBinary -> {
                if (((AttributesSchemaInternal) project.getDependencies().getAttributesSchema()).matcher().isMatching((AttributeContainerInternal) binary.getLinkConfiguration().getAttributes(), (AttributeContainerInternal) ((Configuration) testedBinary.getLinkLibraries()).getAttributes())) {
                    ConfigurableFileCollection testableObjects = project.getObjects().fileCollection();
                    if (component instanceof CppApplication) {
                        TaskProvider<UnexportMainSymbol> unexportMainSymbol = project.getTasks().register(binary.getNames().getTaskName("relocateMainFor"), UnexportMainSymbol.class, (task) -> {
                            String dirName = ((DefaultCppBinary) testedBinary).getNames().getDirName();
                            task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("obj/for-test/" + dirName));
                            task.getObjects().from(testedBinary.getObjects());
                        });
                        testableObjects.from(unexportMainSymbol.map(UnexportMainSymbol::getRelocatedObjects));
                    } else {
                        testableObjects.from(testedBinary.getObjects());
                    }
                    //binary.getImplementationDependencies().extendsFrom(((DefaultCppBinary) testedBinary).getImplementationDependencies());

                    Dependency linkDependency = project.getDependencies().create(testableObjects);
                    binary.getLinkConfiguration().getDependencies().add(linkDependency);
                }
            });
            testSuite.getTestedComponent().convention(component);
        });
    }

}
