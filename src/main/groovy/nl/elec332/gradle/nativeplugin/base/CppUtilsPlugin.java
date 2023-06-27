package nl.elec332.gradle.nativeplugin.base;

import nl.elec332.gradle.nativeplugin.util.Constants;
import nl.elec332.gradle.util.PluginHelper;
import nl.elec332.gradle.util.ProjectHelper;
import nl.elec332.gradle.util.Utils;
import nl.elec332.gradle.util.abstraction.IProjectObjects;
import nl.elec332.gradle.util.internal.GradleCoreInternals;
import nl.elec332.gradle.util.internal.ProjectObjects;
import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.language.ComponentWithBinaries;
import org.gradle.language.cpp.*;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.nativeplatform.tasks.LinkExecutable;
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

    private static final String LINK = "link";
    public static final String LINKER = LINK + "er";
    public static final String RUNTIME = "cppRuntime";

    public static final String LINKER_RELEASE = LINK + "Release";
    public static final String RUNTIME_RELEASE = RUNTIME + "Release";
    public static final String LINKER_DEBUG = LINK + "Debug";
    public static final String RUNTIME_DEBUG = RUNTIME + "Debug";

    public static Usage CPP_API_USAGE;
    public static Usage LINK_USAGE;
    public static Usage RUNTIME_USAGE;

    public static IProjectObjects PROJECT_OBJECTS;

    private final Map<IComponentConfigurator<?>, Object> componentConfigurators = new LinkedHashMap<>();
    private final Map<IBinaryConfigurator<Object>, Object> binaryConfigurators = new LinkedHashMap<>();
    private final Set<Action<? super CppCompile>> compilerMods = new LinkedHashSet<>();
    private final Set<Action<? super CppComponent>> componentMods = new LinkedHashSet<>();
    private final Set<Action<? super CppBinary>> binaryMods = new LinkedHashSet<>();
    private final Map<Object, Set<Runnable>> callbacks = new WeakHashMap<>();
    private Project project;
    private boolean cpmLock = false;


    private final Set<Action<? super LinkExecutable>> linkerMods = new LinkedHashSet<>();
    private boolean cplLock = false;

    @Override
    public void apply(Project project) {
        PluginHelper.checkMinimumGradleVersion(Constants.GRADLE_VERSION);
        this.project = project;

        CPP_API_USAGE = project.getObjects().named(Usage.class, Usage.C_PLUS_PLUS_API);
        LINK_USAGE = project.getObjects().named(Usage.class, Usage.NATIVE_LINK);
        RUNTIME_USAGE = project.getObjects().named(Usage.class, Usage.NATIVE_RUNTIME);

        PROJECT_OBJECTS = Utils.getProjectObjects(project);

        project.getConfigurations().create(HEADERS);
        project.getConfigurations().create(WINDOWS_HEADERS);

        Configuration link = project.getConfigurations().create(LINKER);
        Configuration runtime = project.getConfigurations().create(RUNTIME);

        project.getConfigurations().create(LINKER_RELEASE).extendsFrom(link);
        project.getConfigurations().create(LINKER_DEBUG).extendsFrom(link);
        project.getConfigurations().create(RUNTIME_RELEASE).extendsFrom(runtime);
        project.getConfigurations().create(RUNTIME_DEBUG).extendsFrom(runtime);

        ((ExtraPropertiesExtension) project.getExtensions().getByName(ExtraPropertiesExtension.EXTENSION_NAME)).set("os", GradleCoreInternals.getCurrentOs());

        //Run binary modifiers
        modifyBinaries(project, binary -> {

            binary.getCompileTask().get().getExtensions().getByType(ExtraPropertiesExtension.class).set("isStatic", binary instanceof CppStaticLibrary);
            binary.getCompileTask().get().source(project.fileTree(ProjectHelper.getDefaultMainSourceFolderPath(project) + "/cpp", f -> f.include("**/*.c")));
            //((CppComponent) softwareComponent).getSource().from(project.fileTree(ProjectHelper.getDefaultMainSourceFolderPath(project) + "/c", f -> f.include("**/*.c")));
            binaryMods.forEach(a -> a.execute(binary));
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

        project.afterEvaluate(p -> {

            //Run component modifiers
            modifyComponents(componentConfigurators);

            project.afterEvaluate(p2 -> {

                //Run compiler modifiers
                project.getTasks().withType(CppCompile.class).configureEach(c -> {
                    cpmLock = true;
                    compilerMods.forEach(a -> a.execute(c));
                });

                //Run compiler modifiers
                project.getTasks().withType(LinkExecutable.class).configureEach(c -> {
                    cplLock = true;
                    linkerMods.forEach(a -> a.execute(c));
                });

            });
        });

        modifyBinaries(binary -> mergeConfigurations(project, binary));
    }

    private void modifyBinaries(Project project, Consumer<CppBinary> consumer) {
        project.getComponents().whenObjectAdded(component -> {
            if (component instanceof ComponentWithBinaries) {
                ((ComponentWithBinaries) component).getBinaries().whenElementFinalized(binary -> {
                    Set<Runnable> callbacks = this.callbacks.getOrDefault(component, Collections.emptySet());
                    callbacks.forEach(Runnable::run);
                    this.callbacks.remove(component);
                    if (binary instanceof CppBinary) {
                        consumer.accept((CppBinary) binary);
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void modifyComponents(Map<IComponentConfigurator<?>, Object> componentConfigurators_) {
        Map<IComponentConfigurator<Object>, Object> componentConfigurators = (Map<IComponentConfigurator<Object>, Object>) (Map<?, ?>) componentConfigurators_; //How about this for a dirty cast?
        project.getComponents().forEach(softwareComponent -> {
            Set<Runnable> callbacks = this.callbacks.computeIfAbsent(softwareComponent, o -> new HashSet<>());
            if (softwareComponent instanceof CppComponent) {
                componentMods.forEach(a -> a.execute((CppComponent) softwareComponent));
                componentMods.clear();
                componentConfigurators.forEach((configurator, data) -> configurator.configureComponent(project, (CppComponent) softwareComponent, callbacks::add, data));
                if (softwareComponent instanceof CppLibrary) {
                    componentConfigurators.forEach((configurator, data) -> configurator.configureLibrary(project, (CppLibrary) softwareComponent, callbacks::add, data));
                } else if (softwareComponent instanceof CppApplication) {
                    componentConfigurators.forEach((configurator, data) -> configurator.configureExecutable(project, (CppApplication) softwareComponent, callbacks::add, data));
                } // No need to alter CppTestSuite (yet)
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
    public <T> void addComponentConfigurator(IComponentConfigurator<T> configurator, T data) {
        componentConfigurators.put(configurator, data);
    }

    @Override
    public <T> void modifyComponentsDirect(IComponentConfigurator<T> configurator, T data) {
        modifyComponents(Collections.singletonMap(configurator, data));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void addBinaryConfigurator(IBinaryConfigurator<T> configurator, T data) {
        binaryConfigurators.put((IBinaryConfigurator<Object>) configurator, data);
    }

    @Override
    public void modifyCompilers(Action<? super CppCompile> action) {
        if (cpmLock) {
            project.getTasks().withType(CppCompile.class).configureEach(action);
            return;
        }
        compilerMods.add(action);
    }

    @Override
    public void modifyLinkers(Action<? super LinkExecutable> action) {
        if (cplLock) {
            project.getTasks().withType(LinkExecutable.class).configureEach(action);
            return;
        }
        linkerMods.add(action);
    }

    @Override
    public void modifyComponents(Action<? super CppComponent> action) {
        componentMods.add(action);
    }

    @Override
    public void modifyBinaries(Action<? super CppBinary> action) {
        binaryMods.add(action);
    }

}
