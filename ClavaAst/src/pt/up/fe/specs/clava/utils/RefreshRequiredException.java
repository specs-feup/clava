/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

/** A source action could not update every affected context safely; no partial edit is committed. */
public final class RefreshRequiredException extends IllegalStateException {
    public RefreshRequiredException(String reason) { super("Compiler refresh required: " + reason); }
}
