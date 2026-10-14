/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.utils.ClassesService;

class ClavaNodeParserLifecycleTest {

    @Test
    void closeRunsAndReleasesActionsAfterSuccessAndFailure() {
        ClangAstData data = new ClangAstData();
        ClavaNodeParser parser = new ClavaNodeParser(new ClassesService());
        parser.init(dataWithContext(data));
        AtomicInteger completedActions = new AtomicInteger();

        data.get(ClangAstData.CLAVA_NODES).queueAction(completedActions::incrementAndGet);
        parser.close(data);

        assertEquals(1, completedActions.get());
        assertTrue(data.get(ClangAstData.CLAVA_NODES).getQueuedActions().isEmpty());

        data.get(ClangAstData.CLAVA_NODES).queueAction(() -> {
            throw new IllegalStateException("rejected parser action");
        });
        assertThrows(IllegalStateException.class, () -> parser.close(data));
        assertTrue(data.get(ClangAstData.CLAVA_NODES).getQueuedActions().isEmpty());

        data.get(ClangAstData.CLAVA_NODES).queueAction(completedActions::incrementAndGet);
        parser.close(data);
        parser.close(data);

        assertEquals(2, completedActions.get());
        assertTrue(data.get(ClangAstData.CLAVA_NODES).getQueuedActions().isEmpty());
    }

    private static ClangAstData dataWithContext(ClangAstData data) {
        data.set(ClangAstData.CONTEXT, new ClavaContext());
        return data;
    }
}
