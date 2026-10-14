/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateNodeDataTest {
    @TempDir Path temporary;
    private final String schema = System.getProperty("node.schema");
    private final String contract = System.getProperty("node.contract");

    @Test
    void fieldReviewIsExplicitAndMismatchPersistsAcrossBuilds() throws Exception {
        var lock = temporary.resolve("fields.lock");
        assertThrows(IllegalStateException.class, () -> run("verify", lock));
        assertFalse(Files.exists(lock));
        run("update", lock);
        String accepted = Files.readString(lock);
        run("verify", lock);
        for (String changed : new String[] {
                accepted + "astwire.v2.NewData.new_field Bool\n",
                accepted.replaceFirst("(?m)^.*BuiltinTypeData.kind_literal.*\\n", ""),
                accepted.replace("kind_literal String", "kind_literal Long"),
                accepted.replace("slot=1 offset=6", "slot=5 offset=14"),
                accepted.replace("BuiltinKind.Int=", "BuiltinKind.RenamedInt=") }) {
            assertNotEquals(accepted, changed);
            Files.writeString(lock, changed);
            run("warn", lock);
            assertEquals(changed, Files.readString(lock));
            assertThrows(IllegalStateException.class, () -> run("verify", lock));
            assertThrows(IllegalStateException.class, () -> run("verify", lock));
        }
        run("update", lock);
        assertEquals(accepted, Files.readString(lock));
        run("verify", lock);
    }

    @Test
    void generationIsDeterministicAndRemovesObsoleteOutput() throws Exception {
        var output = temporary.resolve("sources");
        run("generate", output);
        var base = output.resolve("pt/up/fe/specs/clava/ast/type/PointerTypeBase.java");
        String first = Files.readString(base);
        assertTrue(first.contains("abstract class PointerTypeBase extends Type"));
        assertTrue(first.contains("Type pointeeType"));
        assertTrue(first.contains("Class<? super PointerType> WIRE_PARENT"));
        Files.writeString(output.resolve("obsolete.java"), "old");
        run("generate", output);
        assertEquals(first, Files.readString(base));
        assertFalse(Files.exists(output.resolve("obsolete.java")));
    }

    @Test
    void aRemovedOrRenamedFieldCannotSurviveAsAStaleJavaPolicy() throws Exception {
        var stale = temporary.resolve("node-data.json");
        Files.writeString(stale, Files.readString(Path.of(contract))
                .replace("\"wireField\": \"pointee_type\"", "\"wireField\": \"removed_pointee\""));
        var failure = assertThrows(IllegalStateException.class, () -> GenerateNodeData.main(new String[] {
                "generate", schema, stale.toString(), temporary.resolve("stale-output").toString() }));
        assertTrue(failure.getMessage().contains("Obsolete Java field policy"));
    }

    private void run(String mode, Path destination) throws Exception {
        GenerateNodeData.main(new String[] { mode, schema, contract, destination.toString() });
    }
}
