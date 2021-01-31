package nl.elec332.gradle.nativeplugin.util;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 7-1-2021
 */
public class Constants {

    public static final String GRADLE_VERSION = "6.0";
    public static final Collection<String> WINDOWS_INCLUDES, LINUX_INCLUDES;

    public static final String CMAKE_RELEASE_CONFIG = "Release";
    public static final String CMAKE_DEBUG_CONFIG = "Debug";
    public static final String DEFAULT_INCLUDE_FOLDER = "include";

    public static final File NULL_FILE = new File("NULL_FILE_OBJ");

    static {
        WINDOWS_INCLUDES = Collections.unmodifiableSet(Stream.concat(
                Stream.of("kernel", "user", "gdi", "shell", "ole", "oleaut", "comdlg", "advapi")
                        .map(s -> s + "32.lib"),
                Stream.of("winspool.lib", "uuid.lib"))
                .collect(Collectors.toSet()));
        LINUX_INCLUDES = Collections.unmodifiableList(Arrays.asList("-lstdc++fs", "-pthread", "-ldl"));
    }

}
