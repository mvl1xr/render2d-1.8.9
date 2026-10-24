import net.minecraft.client.main.Main;

import java.io.File;
import java.util.Arrays;

public class Start {
    public static void main(String[] args) {
        String currentDir = System.getProperty("user.dir");
        System.out.println("Current working directory: " + currentDir);

        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getenv("PROCESSOR_ARCHITECTURE");
        if (osArch == null) {
            osArch = System.getenv("PROCESSOR_ARCHITEW6432");
        }
        if (osArch == null) {
            osArch = System.getProperty("os.arch").toLowerCase();
        }

        String nativeSubDir;
        if (osName.contains("win")) {
            nativeSubDir = "windows";
        } else if (osName.contains("mac")) {
            if (osArch.contains("aarch64") || osArch.contains("arm64")) {
                nativeSubDir = "osx_silicon";
            } else {
                nativeSubDir = "osx";
            }
        } else if (osName.contains("nix") || osName.contains("nux")) {
            nativeSubDir = "linux";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        File currentDirFile = new File(currentDir);
        File[] candidateNativeDirs = new File[] {
            new File(currentDirFile, "natives/" + nativeSubDir),
            new File(currentDirFile.getParentFile(), "natives/" + nativeSubDir),
            new File(currentDirFile, "run/natives/" + nativeSubDir)
        };

        File nativeDir = null;
        for (File candidate : candidateNativeDirs) {
            if (candidate.exists()) {
                nativeDir = candidate;
                break;
            }
        }

        if (nativeDir == null) {
            System.err.println("Native library directory not found for: " + nativeSubDir);
            return;
        }

        String absoluteNativePath = nativeDir.getAbsolutePath();
        System.out.println("Native library path: " + absoluteNativePath);
        System.setProperty("org.lwjgl.librarypath", absoluteNativePath);

        File gameDir = new File(currentDirFile, "run");
        if (!gameDir.exists()) {
            gameDir = currentDirFile;
        }

        File assetsDir = new File(gameDir, "assets");
        if (!assetsDir.exists()) {
            assetsDir = new File(currentDirFile, "assets");
        }

        System.out.println("Game directory: " + gameDir.getAbsolutePath());
        System.out.println("Assets directory: " + assetsDir.getAbsolutePath());

        Main.main(concat(new String[]{
                        "--version", "amoral",
                        "--accessToken", "0",
                        "--gameDir", gameDir.getAbsolutePath(),
                        "--assetsDir", assetsDir.getAbsolutePath(),
                        "--assetIndex", "1.8",
                        "--userProperties", "{}"},
                args)
        );
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
