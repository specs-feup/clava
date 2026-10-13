/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clava.language;

import pt.up.fe.specs.util.providers.StringProvider;

/** The source-location builtin represented by Clang's {@code SourceLocExpr}. */
public enum SourceLocIdentKind implements StringProvider {
    Function("__builtin_FUNCTION"),
    FuncSig("__builtin_FUNCSIG"),
    File("__builtin_FILE"),
    FileName("__builtin_FILE_NAME"),
    Line("__builtin_LINE"),
    Column("__builtin_COLUMN"),
    SourceLocStruct("__builtin_source_location");

    private final String builtinName;

    SourceLocIdentKind(String builtinName) {
        this.builtinName = builtinName;
    }

    @Override
    public String getString() {
        return builtinName;
    }
}
