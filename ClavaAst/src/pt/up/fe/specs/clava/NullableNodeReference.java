/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */

package pt.up.fe.specs.clava;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a node DataKey whose absent reference is represented by a Clava null node. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NullableNodeReference {
}
