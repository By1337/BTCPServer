package org.by1337.btcp.server.util.resource;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class ResourceUtil {
    @CanIgnoreReturnValue
    public static File saveResource(@NotNull String resourcePath, boolean replace, File dataFolder) {
        return saveResource(resourcePath, replace, dataFolder, ResourceUtil.class);
    }

    @CanIgnoreReturnValue
    public static File saveResource(@NotNull String resourcePath, boolean replace, File dataFolder, Class<?> clazz) {
        InputStream in = getResource(resourcePath, clazz);
        File outFile = new File(dataFolder, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(dataFolder, resourcePath.substring(0, Math.max(lastIndex, 0)));
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
            return outFile;
        } catch (IOException ex) {
            throw new RuntimeException("Could not saveAndPrint " + outFile.getName() + " to " + outFile, ex);
        }
    }

    private static @Nullable InputStream getResource(@NotNull String filename, Class<?> clazz) {
        try {
            URL url = clazz.getClassLoader().getResource(filename);
            if (url == null) {
                return null;
            }
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
    public static String getResourceAsString(@NotNull String filename) {
        return getResourceAsString(filename, ResourceUtil.class);
    }
    public static String getResourceAsString(@NotNull String filename, Class<?> clazz) {
        try {
            URL url = clazz.getClassLoader().getResource(filename);
            if (url == null) {
                return null;
            }
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            try (InputStream in = connection.getInputStream()){
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
