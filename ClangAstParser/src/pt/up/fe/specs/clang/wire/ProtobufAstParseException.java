/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */

package pt.up.fe.specs.clang.wire;

/** Identifies a failure while decoding a clang-dumper Protobuf AST stream. */
public final class ProtobufAstParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProtobufAstParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
