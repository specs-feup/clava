/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang.parsers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.context.ClavaContext;

public class ClavaNodesTest {

    @Test
    public void optionalNodeResolvesPresentNode() {
        var context = new ClavaContext();
        var factory = context.getFactory();
        var clavaNodes = new ClavaNodes(factory);
        DataStore data = factory.newDataStore(Expr.class);
        var type = factory.nullType();

        clavaNodes.getNodes().put("type-id", type);
        clavaNodes.queueSetOptionalNode(data, Expr.TYPE, "type-id");
        clavaNodes.getQueuedActions().forEach(Runnable::run);

        assertSame(type, data.get(Expr.TYPE).orElseThrow());
    }

    @Test
    public void optionalNodeUsesEmptyForExplicitNullId() {
        var context = new ClavaContext();
        var factory = context.getFactory();
        var clavaNodes = new ClavaNodes(factory);
        DataStore data = factory.newDataStore(Expr.class);

        clavaNodes.queueSetOptionalNode(data, Expr.TYPE, "nullptr_type");
        clavaNodes.getQueuedActions().forEach(Runnable::run);

        assertTrue(data.get(Expr.TYPE).isEmpty());
    }

    @Test
    public void optionalNodeRejectsUnresolvedNodeId() {
        var context = new ClavaContext();
        var factory = context.getFactory();
        var clavaNodes = new ClavaNodes(factory);
        DataStore data = factory.newDataStore(Expr.class);

        clavaNodes.queueSetOptionalNode(data, Expr.TYPE, "missing-id");

        var exception = assertThrows(NullPointerException.class,
                () -> clavaNodes.getQueuedActions().forEach(Runnable::run));

        assertTrue(exception.getMessage().contains("Could not resolve optional node 'missing-id'"));
        assertTrue(exception.getMessage().contains("key 'type'"));
    }
}
