package nl.elec332.gradle.nativeplugin.common;

import nl.elec332.gradle.nativeplugin.api.INativeProjectDependencyHandler;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 5-1-2021
 */
public class NativeProjectDependencyHandler implements INativeProjectDependencyHandler {

    NativeProjectDependencyHandler(ObjectFactory objectFactory, Project project) {
        winInc = objectFactory.setProperty(String.class);
        linInc = objectFactory.setProperty(String.class);
        pkgs = new HashSet<>();
        winLink = new HashSet<>();
        linLink = new HashSet<>();

        winInc.set(Stream.of("kernel", "user", "gdi", "shell", "ole", "oleaut", "comdlg", "advapi").map(s -> s + "32.lib").collect(Collectors.toSet()));
        winInc.addAll(Arrays.asList("winspool.lib", "uuid.lib"));
        linInc.set(Arrays.asList("-lstdc++fs", "-pthread", "-ldl"));
    }

    private final SetProperty<String> winInc;
    private final SetProperty<String> linInc;
    private final Set<String> pkgs, winLink, linLink;

    @Override
    public SetProperty<String> getDefaultWindowsIncludes() {
        return winInc;
    }

    @Override
    public SetProperty<String> getDefaultLinuxIncludes() {
        return linInc;
    }

    @Override
    public void linuxPkgDependency(String name) {
        pkgs.add(name);
    }

    @Override
    public void windowsLinker(String arg) {
        winLink.add(arg);
    }

    @Override
    public void linuxLinker(String arg) {
        linLink.add(arg);
    }

    public Set<String> getLinuxPkgDeps() {
        return pkgs;
    }

    public Set<String> getWinLink() {
        return winLink;
    }

    public Set<String> getLinLink() {
        return linLink;
    }

}
