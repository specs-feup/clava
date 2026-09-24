package pt.up.fe.specs.clang.dumper;

import org.junit.jupiter.api.Test;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import org.suikasoft.jOptions.Interfaces.DataStore;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Emits same-process parser snapshots for the scratch text/Protobuf fidelity gate. */
class AstWireFidelitySnapshotTest {

    @Test
    void writesCAndCxxSnapshotsForSelectedWireMode() throws Exception {
        String wire = requiredEnvironment("CLAVA_AST_AB_WIRE");
        assertTrue(wire.equals("text") || wire.equals("protobuf"),
                "CLAVA_AST_AB_WIRE must be text or protobuf");
        System.setProperty("clava.astAbWire", wire);

        Path cFixture = Path.of(requiredEnvironment("CLAVA_AST_AB_FIXTURE_C")).toRealPath();
        Path cxxFixture = Path.of(requiredEnvironment("CLAVA_AST_AB_FIXTURE_CXX")).toRealPath();
        Path outputRoot = Path.of(requiredEnvironment("CLAVA_AST_AB_SNAPSHOT_DIR")).toAbsolutePath();
        Path modeDirectory = outputRoot.resolve(wire);
        Files.createDirectories(modeDirectory);

        File tool = nativeTool();
        assertTrue(tool.isFile(), "CLANG_DUMPER_TOOL must point to the scratch native binary");

        writeSnapshot(tool, cFixture, Standard.C11, modeDirectory.resolve("c.json"));
        writeSnapshot(tool, cxxFixture, Standard.CXX17, modeDirectory.resolve("cxx.json"));
    }

    private static void writeSnapshot(File tool, Path fixture, Standard standard, Path output) throws Exception {
        var dumper = new ClangAstDumper(false, tool, List.of(), null, new ParallelCodeParser());
        ClangAstData data = dumper.parse(fixture.toFile(), "42", standard, config());
        AstWireGraphSnapshot.write(data.get(ClangAstData.TRANSLATION_UNIT), fixture, output);
    }

    private static DataStore config() {
        var config = ClangAstKeys.toDataStore(List.of());
        config.set(ClangAstKeys.USES_CILK, false);
        config.set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.SYSTEM);
        config.add(ClavaNode.CONTEXT, new ClavaContext());
        return config;
    }

    private static File nativeTool() {
        String configured = System.getenv("CLANG_DUMPER_TOOL");
        assertTrue(configured != null && !configured.isBlank(), "CLANG_DUMPER_TOOL is required");
        return Path.of(configured).toAbsolutePath().toFile();
    }

    private static String requiredEnvironment(String key) {
        String value = System.getenv(key);
        assertTrue(value != null && !value.isBlank(), key + " is required");
        return value;
    }
}
