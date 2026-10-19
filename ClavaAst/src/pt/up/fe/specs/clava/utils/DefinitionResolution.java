/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.util.List;
import java.util.Optional;

/** An inspectable lookup result; conflicting definitions are different from an absent definition. */
public record DefinitionResolution<T>(Kind kind, List<T> candidates) {
    public enum Kind { MISSING, UNIQUE, AMBIGUOUS }
    public DefinitionResolution {
        java.util.Objects.requireNonNull(kind);
        candidates = List.copyOf(candidates);
        if ((kind == Kind.MISSING && !candidates.isEmpty()) || (kind == Kind.UNIQUE && candidates.size() != 1)
                || (kind == Kind.AMBIGUOUS && candidates.size() < 2))
            throw new IllegalArgumentException("Candidate count does not match resolution kind " + kind);
    }
    public Optional<T> unique() { return kind == Kind.UNIQUE ? Optional.of(candidates.get(0)) : Optional.empty(); }
}
