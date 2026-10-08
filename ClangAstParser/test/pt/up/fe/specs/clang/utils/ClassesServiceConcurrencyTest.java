/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.utils.ClassesService;

class ClassesServiceConcurrencyTest {

    @Test
    void concurrentFirstLookupsResolveTheSameNodeClasses() throws Exception {
        ClassesService service = new ClassesService();
        List<String> classNames = List.of("FunctionDecl", "VarDecl", "BuiltinType", "IntegerLiteral");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<Class<? extends ClavaNode>>> lookups = new ArrayList<>();
            List<Class<? extends ClavaNode>> expected = new ArrayList<>();

            for (int index = 0; index < 256; index++) {
                String className = classNames.get(index % classNames.size());
                Class<? extends ClavaNode> expectedClass = switch (className) {
                    case "FunctionDecl" -> pt.up.fe.specs.clava.ast.decl.FunctionDecl.class;
                    case "VarDecl" -> pt.up.fe.specs.clava.ast.decl.VarDecl.class;
                    case "BuiltinType" -> pt.up.fe.specs.clava.ast.type.BuiltinType.class;
                    case "IntegerLiteral" -> pt.up.fe.specs.clava.ast.expr.IntegerLiteral.class;
                    default -> throw new IllegalStateException(className);
                };
                expected.add(expectedClass);
                lookups.add(workers.submit(() -> {
                    start.await();
                    return service.getClass(className, DataStore.newInstance("Concurrent class lookup"));
                }));
            }

            start.countDown();
            for (int index = 0; index < lookups.size(); index++) {
                assertEquals(expected.get(index), lookups.get(index).get());
            }
        } finally {
            workers.shutdownNow();
        }
    }
}
