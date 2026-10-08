/**
 * Copyright 2017 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxArrayAccess;
import pt.up.fe.specs.util.SpecsCollections;

public class CxxSelects {

    /**
     * Selects join points.
     *
     *
     * @param targetJoinpoint
     * @param directChildren
     * @param selectDescendents
     * @param filter
     * @return
     */
    public static <T extends AJoinpoint<?>> T[] select(CxxWeaver weaver, Class<T> targetJoinpoint,
            List<? extends ClavaNode> directChildren, boolean selectDescendents, Predicate<? super ClavaNode> filter) {

        Stream<? extends ClavaNode> currentStream = directChildren.stream();
        if (selectDescendents) {
            currentStream = currentStream.flatMap(node -> node.getDescendantsAndSelfStream());
        }

        return currentStream.filter(filter)
                .map(node -> CxxJoinpoints.create(node, weaver, targetJoinpoint))
                // Filter null join points
                .filter(jp -> jp != null)
                .toArray(size -> SpecsCollections.newArray(targetJoinpoint, size));
    }

    public static <T extends AJoinpoint<?>> T[] select(CxxWeaver weaver, Class<T> targetJoinpoint,
            List<? extends ClavaNode> directChildren, boolean selectDescendents, Class<? extends ClavaNode> filter) {

        return select(weaver, targetJoinpoint, directChildren, selectDescendents, filter::isInstance);
    }

    public static boolean stmtFilter(ClavaNode node) {
        if (!(node instanceof Stmt)) {
            return false;
        }

        Stmt stmt = (Stmt) node;

        if (stmt.isAggregateStmt()) {
            return false;
        }

        if (stmt instanceof LoopStmt || stmt instanceof IfStmt) {
            return false;
        }

        // Wrapper statements (which encapsulate pragmas and comments) should be ignored,
        // comments and pragmas are not statements, should be selected with their respective join points
        if (stmt instanceof WrapperStmt) {
            return false;
        }

        return true;
    }

    public static AJoinpoint<?>[] selectedNodesToJps(Stream<? extends ClavaNode> selectedNodes,
            CxxWeaver weaverEngine) {
        return selectedNodesToJps(selectedNodes, jp -> true, weaverEngine);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AJoinpoint<?>> T[] selectedNodesToJps(Stream<? extends ClavaNode> selectedNodes,
            Predicate<T> filter, CxxWeaver weaverEngine) {

        return selectedNodesToJpsStream(selectedNodes, filter, weaverEngine)
                // Collect to list first, to avoid issues with generic array creation
                .collect(Collectors.toList())
                .toArray(size -> (T[]) new AJoinpoint<?>[size]);
    }

    public static Stream<AJoinpoint<?>> selectedNodesToJpsStream(Stream<? extends ClavaNode> selectedNodes,
            CxxWeaver weaverEngine) {

        return selectedNodesToJpsStream(selectedNodes, jp -> true, weaverEngine);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AJoinpoint<?>> Stream<T> selectedNodesToJpsStream(Stream<? extends ClavaNode> selectedNodes,
            Predicate<T> filter, CxxWeaver weaverEngine) {

        return selectedNodes
                // Ignore null nodes
                .filter(sibling -> !(sibling instanceof NullNode))
                .map(node -> CxxJoinpoints.create(node, weaverEngine))
                // Filter null nodes
                .filter(jp -> jp != null)
                // Default filter
                .filter(CxxSelects::defaultSelectFilter)
                .map(jp -> (T) jp)
                .filter(filter);
    }

    private static boolean defaultSelectFilter(AJoinpoint<?> jp) {
        // TODO: If more cases, use a ClassMap instead

        // If ArraySubscript, return only if top-level
        if (jp instanceof CxxArrayAccess) {
            return ((ArraySubscriptExpr) jp.getNodeImpl()).isTopLevel();
        }

        return true;
    }
}
