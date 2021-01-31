package nl.elec332.gradle.nativeplugin.base;

import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.util.PluginHelper;
import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.language.ComponentWithBinaries;
import org.gradle.language.cpp.*;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.test.cpp.CppTestExecutable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by Elec332 on 1/30/2021
 */
@NonNullApi
public class CppUtilsPlugin implements ICppUtilsPlugin, Plugin<Project> {

    public static final String HEADERS = "headers";
    public static final String WINDOWS_HEADERS = "windowsHeaders";

    public static final String LINKER = "linker";
    public static final String RUNTIME = "runtime";

    public static final String LINKER_RELEASE = "linkRelease";
    public static final String RUNTIME_RELEASE = "runtimeRelease";
    public static final String LINKER_DEBUG = "linkDebug";
    public static final String RUNTIME_DEBUG = "runtimeDebug";


    private final Map<IComponentConfigurator<Object>, Object> componentConfigurators = new LinkedHashMap<>();
    private final Map<IBinaryConfigurator<Object>, Object> binaryConfigurators = new LinkedHashMap<>();
    private final Set<Action<? super CppCompile>> compilerMods = new LinkedHashSet<>();

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);

        project.getConfigurations().create(HEADERS);
        project.getConfigurations().create(WINDOWS_HEADERS);

        Configuration link = project.getConfigurations().create(LINKER);
        Configuration runtime = project.getConfigurations().create(RUNTIME);

        project.getConfigurations().create(LINKER_RELEASE).extendsFrom(link);
        project.getConfigurations().create(LINKER_DEBUG).extendsFrom(link);
        project.getConfigurations().create(RUNTIME_RELEASE).extendsFrom(runtime);
        project.getConfigurations().create(RUNTIME_DEBUG).extendsFrom(runtime);

        Set<Runnable> callbacks = new HashSet<>();
        project.afterEvaluate(p -> {

            //Run component modifiers
            project.getComponents().forEach(softwareComponent -> {
                if (softwareComponent instanceof CppComponent) {
                    componentConfigurators.forEach((configurator, data) -> configurator.configureComponent(project, (CppComponent) softwareComponent, callbacks::add, data));
                    if (softwareComponent instanceof CppLibrary) {
                        componentConfigurators.forEach((configurator, data) -> configurator.configureLibrary(project, (CppLibrary) softwareComponent, callbacks::add, data));
                    } else if (softwareComponent instanceof CppApplication) {
                        componentConfigurators.forEach((configurator, data) -> configurator.configureExecutable(project, (CppApplication) softwareComponent, callbacks::add, data));
                    } // No need to alter CppTestSuite (yet)
                }
            });

            //Run binary modifiers
            modifyBinaries(project, binary -> {
                binaryConfigurators.forEach((configurator, data) -> configurator.configureBinary(project, binary, data));
                if (binary instanceof CppStaticLibrary) {
                    CppStaticLibrary lib = (CppStaticLibrary) binary;
                    binaryConfigurators.forEach((configurator, data) -> configurator.configureStaticLibraryBinary(project, lib, data));
                    binaryConfigurators.forEach((configurator, data) -> configurator.configureLibraryBinary(project, lib, data));
                    binaryConfigurators.forEach((configurator, data) -> configurator.configurePublishableBinary(project, lib, data));
                } else if (binary instanceof CppSharedLibrary) {
                    CppSharedLibrary lib = (CppSharedLibrary) binary;
                    binaryConfigurators.forEach((configurator, data) -> configurator.configureSharedLibraryBinary(project, lib, data));
                    binaryConfigurators.forEach((configurator, data) -> configurator.configureLibraryBinary(project, lib, data));
                    binaryConfigurators.forEach((configurator, data) -> configurator.configurePublishableBinary(project, lib, data));
                } else if (binary instanceof CppExecutable) {
                    CppExecutable executable = (CppExecutable) binary;
                    binaryConfigurators.forEach((configurator, data) -> configurator.configurePublishableExecutableBinary(project, executable, data));
                    binaryConfigurators.forEach((configurator, data) -> configurator.configureExecutableBinary(project, executable, data));
                    binaryConfigurators.forEach((configurator, data) -> configurator.configurePublishableBinary(project, executable, data));
                } else if (binary instanceof CppTestExecutable) {
                    CppTestExecutable executable = (CppTestExecutable) binary;
                    binaryConfigurators.forEach((configurator, data) -> configurator.configureTestExecutableBinary(project, executable, data));
                    binaryConfigurators.forEach((configurator, data) -> configurator.configureExecutableBinary(project, executable, data));
                } else {
                    throw new UnsupportedOperationException("Unknown library type: " + binary.getClass());
                }
            });

            //Run compiler modifiers
            project.getTasks().withType(CppCompile.class).configureEach(c -> compilerMods.forEach(a -> a.execute(c)));

            //Run callbacks
            project.afterEvaluate(p2 -> callbacks.forEach(Runnable::run));
        });

        addBinaryConfigurator(new IBinaryConfigurator<Object>() {

            @Override
            public void configureBinary(Project project, CppBinary binary, Object data) {
                mergeConfigurations(project, binary);
            }

        }, null);
    }

    private static void modifyBinaries(Project project, Consumer<CppBinary> consumer) {
        project.getComponents().forEach(component -> {
            if (component instanceof ComponentWithBinaries) {
                ((ComponentWithBinaries) component).getBinaries().whenElementFinalized(softwareComponent -> {
                    if (softwareComponent instanceof CppBinary) {
                        CppBinary binary = (CppBinary) softwareComponent;
                        consumer.accept(binary);
                    }
                });
            }
        });
    }

    private static void mergeConfigurations(Project project, CppBinary binary) {
        Configuration link = (Configuration) binary.getLinkLibraries();
        Configuration runtime = (Configuration) binary.getRuntimeLibraries();

        extendConfiguration(link, project.getConfigurations().getByName(CppUtilsPlugin.LINKER_RELEASE), project.getConfigurations().getByName(CppUtilsPlugin.LINKER_DEBUG));
        extendConfiguration(runtime, project.getConfigurations().getByName(CppUtilsPlugin.RUNTIME_RELEASE), project.getConfigurations().getByName(CppUtilsPlugin.RUNTIME_DEBUG));

        binary.getCompileTask().get().includes(project.getConfigurations().getByName(CppUtilsPlugin.HEADERS));
        if (binary.getTargetPlatform().getTargetMachine().getOperatingSystemFamily().isWindows()) {
            binary.getCompileTask().get().includes(project.getConfigurations().getByName(CppUtilsPlugin.WINDOWS_HEADERS));
        }
    }

    private static void extendConfiguration(Configuration test, Configuration release, Configuration debug) {
        if (test.getName().toLowerCase(Locale.ROOT).contains("release")) {
            test.extendsFrom(release);
        } else if (test.getName().toLowerCase(Locale.ROOT).contains("debug")) {
            test.extendsFrom(debug);
        } else {
            Boolean o = test.getAttributes().getAttribute(CppBinary.OPTIMIZED_ATTRIBUTE);
            if (o != null && o) {
                test.extendsFrom(release);
            } else {
                test.extendsFrom(debug);
            }
        }
    }

    public static ICppUtilsPlugin getBasePlugin(Project project) {
        return project.getPlugins().getPlugin(CppUtilsPlugin.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void addComponentConfigurator(IComponentConfigurator<T> configurator, T data) {
        componentConfigurators.put((IComponentConfigurator<Object>) configurator, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void addBinaryConfigurator(IBinaryConfigurator<T> configurator, T data) {
        binaryConfigurators.put((IBinaryConfigurator<Object>) configurator, data);
    }

    @Override
    public void modifyCompiler(Action<? super CppCompile> action) {
        compilerMods.add(action);
    }
}
