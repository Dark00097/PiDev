package com.myapp.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class WindowsBiometricAuth {
    private static final String DEFAULT_PROMPT = "NEXORA Bank admin verification";
    private static final String HELPER_NAME = "HelloAuth.exe";
    private static final Path HELPER_RELEASE_DIR = Paths.get("native", "HelloAuth", "bin", "Release");

    private static volatile String lastStdout = "";
    private static volatile String lastStderr = "";
    private static volatile String lastDebug = "";
    private static volatile Path lastExecutablePath;

    private WindowsBiometricAuth() {
    }

    public static AuthResult verify(String message) {
        String prompt = (message == null || message.isBlank()) ? DEFAULT_PROMPT : message;
        lastStdout = "";
        lastStderr = "";
        lastDebug = "";
        lastExecutablePath = null;

        Path executablePath = resolveHelperExecutable();
        if (executablePath == null) {
            lastDebug = "Biometric helper executable not found. "
                + "Expected under ./" + HELPER_RELEASE_DIR + "/<target-framework>/" + HELPER_NAME + ". "
                + "Build with: " + getBuildCommandsInline();
            return AuthResult.ERROR;
        }

        lastExecutablePath = executablePath.toAbsolutePath().normalize();
        ProcessBuilder processBuilder = new ProcessBuilder(executablePath.toString(), prompt);
        processBuilder.redirectErrorStream(false);

        Process process = null;
        ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
        try {
            process = processBuilder.start();

            Process processRef = process;
            Future<String> stdoutFuture = ioExecutor.submit(() -> readStream(processRef.getInputStream()));
            Future<String> stderrFuture = ioExecutor.submit(() -> readStream(processRef.getErrorStream()));

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                lastDebug = "Biometric helper timed out after 30 seconds.";
                return AuthResult.ERROR;
            }

            lastStdout = stdoutFuture.get(2, TimeUnit.SECONDS);
            lastStderr = stderrFuture.get(2, TimeUnit.SECONDS);

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return AuthResult.VERIFIED;
            }
            if (exitCode == 2) {
                return AuthResult.NOT_AVAILABLE;
            }
            return AuthResult.FAILED;
        } catch (Exception ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            lastDebug = "Failed to execute biometric helper: " + ex.getMessage();
            return AuthResult.ERROR;
        } finally {
            ioExecutor.shutdownNow();
        }
    }

    public static String getLastStdout() {
        return lastStdout;
    }

    public static String getLastStderr() {
        return lastStderr;
    }

    public static String getLastDebug() {
        return lastDebug;
    }

    public static Path getLastExecutablePath() {
        return lastExecutablePath;
    }

    public static String getBuildHint() {
        return String.join(System.lineSeparator(),
            "dotnet new console -n HelloAuth",
            "replace Program.cs with the provided code",
            "dotnet build -c Release");
    }

    private static String getBuildCommandsInline() {
        return "dotnet new console -n HelloAuth && dotnet build -c Release";
    }

    private static Path resolveHelperExecutable() {
        for (Path baseDir : candidateBaseDirectories()) {
            Path releaseDir = baseDir.resolve(HELPER_RELEASE_DIR).normalize();
            Path executable = searchExecutableInReleaseTree(releaseDir);
            if (executable != null) {
                return executable;
            }
        }
        return null;
    }

    private static List<Path> candidateBaseDirectories() {
        List<Path> candidates = new ArrayList<>();
        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        candidates.add(userDir);

        Path cursor = userDir;
        for (int i = 0; i < 3 && cursor.getParent() != null; i++) {
            cursor = cursor.getParent();
            candidates.add(cursor);
        }
        return candidates;
    }

    private static Path searchExecutableInReleaseTree(Path releaseDir) {
        if (!Files.isDirectory(releaseDir)) {
            return null;
        }

        try (Stream<Path> stream = Files.walk(releaseDir, 4)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> HELPER_NAME.equalsIgnoreCase(path.getFileName().toString()))
                .sorted(Comparator.comparing((Path p) -> p.toAbsolutePath().toString()).reversed())
                .findFirst()
                .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String readStream(InputStream stream) throws IOException {
        byte[] data = stream.readAllBytes();
        return new String(data, StandardCharsets.UTF_8);
    }
}
