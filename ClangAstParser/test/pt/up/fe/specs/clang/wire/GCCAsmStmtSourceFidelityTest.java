/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import pt.up.fe.specs.clava.ast.stmt.AsmStmt;
import pt.up.fe.specs.clava.ast.stmt.GCCAsmStmt;

class GCCAsmStmtSourceFidelityTest {
    @Test
    void retainsEveryAsmInstructionAsAnEscapedStringLiteral() {
        DataStore data = DataStore.newInstance(StoreDefinitions.fromInterface(GCCAsmStmt.class), true);
        String asmTemplate = "first\n\tsecond \"quoted\" \\path" + (char) 0 + "7" + (char) 1
                + (char) 0x7f;
        data.set(GCCAsmStmt.ASM_STRING, asmTemplate);
        data.set(AsmStmt.IS_VOLATILE, false);
        data.set(AsmStmt.OUTPUTS, List.of());
        data.set(AsmStmt.INPUTS, List.of());
        data.set(AsmStmt.CLOBBERS, List.of());

        GCCAsmStmt statement = new GCCAsmStmt(data, List.of());

        assertEquals("__asm__(\"first\\n\\tsecond \\\"quoted\\\" \\\\path\\0007\\001\\177\");",
                statement.getCode());
    }
}
