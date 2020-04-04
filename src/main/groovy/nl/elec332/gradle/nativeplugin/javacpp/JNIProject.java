package nl.elec332.gradle.nativeplugin.javacpp;

import nl.elec332.gradle.nativeplugin.nativeproject.AbstractNativeProject;

import javax.inject.Inject;

/**
 * Created by Elec332 on 3-4-2020
 */
public class JNIProject extends AbstractNativeProject {

    public JNIProject(String name) {
        super(name);
        this.clazz = name;
    }

    private final String clazz;

    @Inject
    public String classPackage;

    @Override
    protected String getName(String name) {
        return "jni" + name;
    }

    public String getMainClassSlashes() {
        return classPackage.replace(".", "/") + "/" + clazz;
    }

}
