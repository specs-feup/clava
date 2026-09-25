package pt.up.fe.specs.clava.weaver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import pt.up.fe.specs.util.SpecsIo;

/**
 * Closing a weaver must not delete directories relative to the working
 * directory, even when they use names formerly used by the weaver for temporary
 * folders.
 */
public class CxxWeaverCloseTest {

    @Test
    public void testCloseDoesNotDeleteCwdFolders() throws IOException {
        File sentinelWoven = new File("__clava_woven", "sentinel.txt");
        File sentinelSrc = new File("__clava_src", "sentinel.txt");
        try {
            SpecsIo.write(sentinelWoven, "user content");
            SpecsIo.write(sentinelSrc, "user content");

            new CxxWeaver().close();

            assertTrue(sentinelWoven.isFile(), "close() deleted an unrelated '__clava_woven' folder");
            assertTrue(sentinelSrc.isFile(), "close() deleted an unrelated '__clava_src' folder");
            assertEquals("user content", SpecsIo.read(sentinelWoven));
            assertEquals("user content", SpecsIo.read(sentinelSrc));
        } finally {
            Files.deleteIfExists(sentinelWoven.toPath());
            Files.deleteIfExists(sentinelSrc.toPath());
            Files.deleteIfExists(sentinelSrc.getParentFile().toPath());
            Files.deleteIfExists(sentinelWoven.getParentFile().toPath());
        }
    }
}
