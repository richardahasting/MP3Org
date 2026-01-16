package org.hasting.util;

import com.log4rich.Log4Rich;
import com.log4rich.core.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Extracts the bundled fpcalc binary for the current platform.
 *
 * <p>The application bundles fpcalc binaries for:
 * <ul>
 *   <li>macOS ARM64 (Apple Silicon)</li>
 *   <li>macOS x86_64 (Intel)</li>
 *   <li>Linux x86_64</li>
 *   <li>Windows x86_64</li>
 * </ul>
 *
 * <p>On first use, the appropriate binary is extracted to a temporary directory
 * and made executable. The extracted path is cached for the session.
 *
 * @since 2.0.0
 */
public class FpcalcExtractor {

    private static final Logger logger = Log4Rich.getLogger(FpcalcExtractor.class);

    private static Path extractedPath = null;
    private static boolean extractionAttempted = false;

    /**
     * Gets the path to the fpcalc executable.
     *
     * <p>This method tries bundled binary first, then falls back to system PATH.
     *
     * @return the path to fpcalc, or "fpcalc" if using system PATH
     */
    public static synchronized String getFpcalcPath() {
        // Try bundled binary first
        if (!extractionAttempted) {
            extractionAttempted = true;
            extractedPath = extractBundledFpcalc();
        }

        if (extractedPath != null && Files.exists(extractedPath)) {
            return extractedPath.toString();
        }

        // Fall back to system PATH
        logger.info("Using system fpcalc from PATH");
        return "fpcalc";
    }

    /**
     * Extracts the bundled fpcalc binary for the current platform.
     *
     * @return the path to the extracted binary, or null if extraction fails
     */
    private static Path extractBundledFpcalc() {
        String binaryName = getBinaryName();
        if (binaryName == null) {
            logger.warn("No bundled fpcalc binary for this platform: {} / {}",
                System.getProperty("os.name"), System.getProperty("os.arch"));
            return null;
        }

        String resourcePath = "/fpcalc/" + binaryName;
        try (InputStream in = FpcalcExtractor.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.warn("Bundled fpcalc not found in resources: {}", resourcePath);
                return null;
            }

            // Create temp directory that persists for the application session
            Path tempDir = Files.createTempDirectory("mp3org-fpcalc-");
            tempDir.toFile().deleteOnExit();

            // Determine output filename
            String outputName = isWindows() ? "fpcalc.exe" : "fpcalc";
            Path fpcalcPath = tempDir.resolve(outputName);
            fpcalcPath.toFile().deleteOnExit();

            // Extract the binary
            Files.copy(in, fpcalcPath, StandardCopyOption.REPLACE_EXISTING);

            // Make executable on Unix systems
            if (!isWindows()) {
                boolean success = fpcalcPath.toFile().setExecutable(true);
                if (!success) {
                    logger.warn("Failed to set executable permission on {}", fpcalcPath);
                }
            }

            logger.info("Extracted bundled fpcalc to: {}", fpcalcPath);
            return fpcalcPath;

        } catch (IOException e) {
            logger.warn("Failed to extract bundled fpcalc: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Determines the correct binary name for the current platform.
     *
     * @return the binary resource name, or null if platform unsupported
     */
    private static String getBinaryName() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        if (os.contains("mac") || os.contains("darwin")) {
            // macOS
            if (arch.equals("aarch64") || arch.contains("arm")) {
                return "fpcalc-darwin-aarch64";
            } else {
                return "fpcalc-darwin-x86_64";
            }
        } else if (os.contains("linux")) {
            // Linux (only x86_64 bundled)
            if (arch.equals("amd64") || arch.equals("x86_64")) {
                return "fpcalc-linux-x86_64";
            }
        } else if (os.contains("win")) {
            // Windows (only x86_64 bundled)
            if (arch.equals("amd64") || arch.equals("x86_64")) {
                return "fpcalc-windows-x86_64.exe";
            }
        }

        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Checks if a bundled binary is available for the current platform.
     *
     * @return true if a bundled binary exists for this platform
     */
    public static boolean hasBundledBinary() {
        String binaryName = getBinaryName();
        if (binaryName == null) {
            return false;
        }
        return FpcalcExtractor.class.getResource("/fpcalc/" + binaryName) != null;
    }
}
