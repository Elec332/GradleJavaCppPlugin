package nl.elec332.gradle.nativeplugin.javacpp;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * Created by Elec332 on 2/20/2021
 */
public class JavaCppExtension implements IJavaCppExtension {

    @Inject
    @SuppressWarnings("UnstableApiUsage")
    public JavaCppExtension(ObjectFactory objects) {
        this.jCppVersion = objects.property(String.class);
        this.mainClass = objects.property(String.class);

        this.jCppVersion.convention("1.5.5");
    }

    private final Property<String> jCppVersion;
    private final Property<String> mainClass;

    @Override
    public Property<String> getDefaultJavaCPPVersion() {
        return this.jCppVersion;
    }

    @Override
    public Property<String> getMainClass() {
        return this.mainClass;
    }

}
