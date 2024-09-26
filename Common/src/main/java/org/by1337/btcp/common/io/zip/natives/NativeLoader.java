package org.by1337.btcp.common.io.zip.natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class NativeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLoader.class);
    public static final boolean NATIVE_IS_AVAILABLE;


    static {
        if (Boolean.getBoolean("btcp.natives-disable")) {
            NATIVE_IS_AVAILABLE = false;
        } else {
            NATIVE_IS_AVAILABLE = tryLoadNativeLibrary();
        }
    }

    private static boolean tryLoadNativeLibrary() {
        ByteBuf test = Unpooled.directBuffer();
        try {
            if (test.hasMemoryAddress()) {
                String lib = getLib();
                if (lib == null) {
                    return false;
                }
                try {
                    loadNative(lib);
                    return true;
                } catch (Throwable t) {
                    LOGGER.warn("Failed to load native library", t);
                    return false;
                }
            } else {
                return false;
            }
        } finally {
            test.release();
        }
    }

    private static void loadNative(String path) {
        try {
            InputStream nativeLib = NativeLoader.class.getResourceAsStream(path);
            if (nativeLib == null) {
                throw new IllegalStateException("Native library " + path + " not found.");
            }

            Path tempFile = Files.createTempFile("native-", path.substring(path.lastIndexOf('.')));
            Files.copy(nativeLib, tempFile, StandardCopyOption.REPLACE_EXISTING);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }));
            try {
                System.load(tempFile.toAbsolutePath().toString());
            } catch (UnsatisfiedLinkError e) {
                throw new NativeLoadException("Unable to load native " + tempFile.toAbsolutePath(), e);
            }
        } catch (IOException e) {
            throw new NativeLoadException("Unable to copy natives", e);
        }
    }

    @Nullable
    private static String getLib() {
        String osArch = System.getProperty("os.arch", "");

        boolean isAmd64 = osArch.equals("amd64") || osArch.equals("x86_64");
        boolean isAarch64 = osArch.equals("aarch64") || osArch.equals("arm64");
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        boolean isLinux = osName.equalsIgnoreCase("Linux");
        boolean isWindows = osName.contains("windows");
        boolean isMusl;
        if (isLinux) {
            isMusl = isMusl();
        } else {
            isMusl = false;
        }
        if (isWindows) {
            if (isAmd64) {
                return "/win_x86_64/btcp-libdeflate.dll";
            }
        } else if (isLinux) {
            if (isAmd64) {
                if (isMusl) {
                    return "/linux_x86_64/btcp-libdeflate-musl.so";
                }
                return "/linux_x86_64/btcp-libdeflate.so";
            } else if (isAarch64) {
                if (isMusl) {
                    return "/linux_aarch64/btcp-libdeflate-musl.so";
                }
                return "/linux_aarch64/btcp-libdeflate.so";
            }
        }
        return null;
    }

    private static boolean isMusl() {
        try {
            Process process = new ProcessBuilder("ldd", "--version")
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            try (var reader = process.getInputStream()) {
                String output = new String(reader.readAllBytes(), StandardCharsets.UTF_8);
                return output.contains("musl");
            }
        } catch (Exception e) {
            return false;
        }
    }
}
