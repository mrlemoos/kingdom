package dev.mrlemoos.kingdom.build;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

/**
 * Runs in the verify phase after package; guards against incomplete or corrupted shaded JARs.
 */
class PackagedJarIntegrityIT {

    private static final Path JAR = Path.of("target/kingdom-0.1.0-SNAPSHOT.jar");

    private static final List<String> REQUIRED_ENTRIES = List.of(
            "dev/mrlemoos/kingdom/mint/TreasuryLordManagementPolicy.class",
            "dev/mrlemoos/kingdom/mint/TreasuryWithdrawGui.class",
            "dev/mrlemoos/kingdom/economy/EconomyCoordinator$MintMatch.class",
            "dev/mrlemoos/kingdom/economy/income/ActivityCategory.class",
            "dev/mrlemoos/kingdom/election/VillagerMpProfessionMatcher.class",
            "dev/mrlemoos/kingdom/election/VillagerPlayerTradePolicy.class",
            "dev/mrlemoos/shaded/incendo/cloud/CommandManager.class");

    @Test
    void packagedJarContainsCriticalClasses() throws IOException {
        assertTrue(Files.isRegularFile(JAR), () -> "Missing packaged artefact: " + JAR);

        try (JarFile jar = new JarFile(JAR.toFile())) {
            for (String entryName : REQUIRED_ENTRIES) {
                JarEntry entry = jar.getJarEntry(entryName);
                assertNotNull(entry, () -> "Packaged JAR missing entry: " + entryName);
            }
        }
    }

    @Test
    void kingdomCommandUsesCloudExecuteEntryPoint() throws IOException {
        assertTrue(Files.isRegularFile(JAR), () -> "Missing packaged artefact: " + JAR);

        try (JarFile jar = new JarFile(JAR.toFile());
                InputStream in = jar.getInputStream(
                        jar.getJarEntry("dev/mrlemoos/kingdom/command/KingdomCommand.class"))) {
            assertNotNull(in);
            byte[] bytes = in.readAllBytes();
            assertTrue(
                    containsUtf8(bytes, "execute"),
                    "KingdomCommand should expose execute(), not legacy onCommand-only builds");
            assertTrue(
                    !containsUtf8(bytes, "onCommand"),
                    "KingdomCommand should not still implement CommandExecutor.onCommand");
        }
    }

    private static boolean containsUtf8(byte[] bytes, String needle) {
        return new String(bytes).contains(needle);
    }
}
