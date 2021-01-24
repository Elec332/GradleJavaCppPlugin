package nl.elec332.gradle.nativeplugin.common;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.internal.metaobject.DynamicInvokeResult;
import org.gradle.internal.metaobject.MethodAccess;
import org.gradle.util.CollectionUtils;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by Elec332 on 1/22/2021
 */
public class DynamicDependencyMethods implements MethodAccess {

    public DynamicDependencyMethods(Predicate<String> tester, Project project) {
        this.tester = tester;
        this.configurationContainer = project.getConfigurations();
        this.dependencyHandler = project.getDependencies();
    }

    private final Predicate<String> tester;
    private final ConfigurationContainer configurationContainer;
    private final DependencyHandler dependencyHandler;

    public boolean hasMethod(String name, Object... arguments) {
        return arguments.length != 0 && (tester == null || tester.test(name)) && this.configurationContainer.findByName(name) != null;
    }

    public DynamicInvokeResult tryInvokeMethod(String name, Object... arguments) {
        if (arguments.length == 0) {
            return DynamicInvokeResult.notFound();
        } else {
            if ((tester != null && !tester.test(name)) || this.configurationContainer.findByName(name) == null) {
                return DynamicInvokeResult.notFound();
            } else {
                List<?> normalizedArgs = CollectionUtils.flattenCollections(arguments);
                if (normalizedArgs.size() == 2 && normalizedArgs.get(1) instanceof Closure) {
                    return DynamicInvokeResult.found(this.dependencyHandler.add(name, normalizedArgs.get(0), (Closure<?>)normalizedArgs.get(1)));
                } else if (normalizedArgs.size() == 1) {
                    return DynamicInvokeResult.found(this.dependencyHandler.add(name, normalizedArgs.get(0)));
                } else {
                    normalizedArgs.forEach(arg -> this.dependencyHandler.add(name, arg));
                    return DynamicInvokeResult.found();
                }
            }
        }
    }

}
