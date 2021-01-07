package nl.elec332.gradle.nativeplugin.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Elec332 on 7-1-2021
 */
public class Constants {

    public static final Collection<String> WINDOWS_INCLUDES, LINUX_INCLUDES;

    static {
        WINDOWS_INCLUDES = Collections.unmodifiableSet(Stream.concat(
                Stream.of("kernel", "user", "gdi", "shell", "ole", "oleaut", "comdlg", "advapi")
                        .map(s -> s + "32.lib"),
                Stream.of("winspool.lib", "uuid.lib"))
                .collect(Collectors.toSet()));
        LINUX_INCLUDES = Collections.unmodifiableList(Arrays.asList("-lstdc++fs", "-pthread", "-ldl"));
    }

}
