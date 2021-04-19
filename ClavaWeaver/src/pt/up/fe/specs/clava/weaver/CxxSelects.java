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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lara.interpreter.profile.ReportField;
import org.lara.interpreter.weaver.interf.WeaverEngine;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.util.utilities.Incrementer;

public class CxxSelects {

    /**
     * Method that helps selecting join points.
     *
     * @param targetJoinpoint
     * @param directChildren
     * @param selectDescendents
     * @param filter
     * @param mapper
     * @return
     */
    private static <T extends ACxxWeaverJoinPoint> List<? extends T> selectPrivate(Class<T> targetJoinpoint,
            List<? extends ClavaNode> directChildren, boolean selectDescendents, Predicate<? super ClavaNode> filter,
            Function<? super ClavaNode, ? extends T> mapper) {

        Stream<? extends ClavaNode> currentStream = directChildren.stream();
        if (selectDescendents) {
            currentStream = currentStream.flatMap(node -> node.getDescendantsAndSelfStream());
        }

        return currentStream.filter(filter)
                .map(mapper)
                // Filter null join points
                .filter(jp -> jp != null)
                .collect(Collectors.toList());
    }

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
    public static <T extends ACxxWeaverJoinPoint> List<? extends T> select(Class<T> targetJoinpoint,
            List<? extends ClavaNode> directChildren, boolean selectDescendents, Predicate<? super ClavaNode> filter) {

        return selectPrivate(targetJoinpoint, directChildren, selectDescendents, filter,
                node -> CxxJoinpoints.create(node, targetJoinpoint));

    }

    public static <T extends ACxxWeaverJoinPoint> List<? extends T> select(Class<T> targetJoinpoint,
            List<? extends ClavaNode> directChildren, boolean selectDescendents, Class<? extends ClavaNode> filter) {

        return select(targetJoinpoint, directChildren, selectDescendents, filter::isInstance);
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

    // public static AJoinPoint[] selectedNodesToJps(List<? extends ClavaNode> selectedNodes, WeaverEngine weaverEngine)
    // {
    // return selectedNodesToJps(selectedNodes.stream(), jp -> true, weaverEngine);
    // }

    public static AJoinPoint[] selectedNodesToJps(Stream<? extends ClavaNode> selectedNodes,
            WeaverEngine weaverEngine) {
        return selectedNodesToJps(selectedNodes, jp -> true, weaverEngine);
    }

    public static AJoinPoint[] selectedNodesToJps(Stream<? extends ClavaNode> selectedNodes,
            Predicate<AJoinPoint> filter, WeaverEngine weaverEngine) {

        Incrementer excludedJoinpoints = new Incrementer();

        AJoinPoint[] selectedJps = selectedNodes
                // Ignore null nodes
                .filter(sibling -> !(sibling instanceof NullNode))
                .map(CxxJoinpoints::create)
                .filter(jp -> {

                    boolean accepted = filter.test(jp);

                    if (!accepted) {
                        excludedJoinpoints.increment();
                    }
                    return accepted;
                })
                // Null nodes should have been filtered by previous filter
                // .filter(jp -> jp != null)
                .collect(Collectors.toList())
                // .toArray(new AJoinPoint[0]);
                .toArray(AJoinPoint[]::new);

        // Count as selected nodes
        var report = weaverEngine.getWeaverProfiler().getReport();
        report.inc(ReportField.JOIN_POINTS, selectedJps.length + excludedJoinpoints.getCurrent());
        report.inc(ReportField.FILTERED_JOIN_POINTS, selectedJps.length);

        // Count as a select
        report.inc(ReportField.SELECTS);

        return selectedJps;
    }

    /*
    Incrementer nullJoinpoints = new Incrementer();
    Incrementer excludedJoinpoints = new Incrementer();
    AJoinPoint[] descendants = getNode().getDescendantsStream()
            .map(descendant -> CxxJoinpoints.create(descendant))
            .filter(jp -> {
                // Count null join points separately
                if (jp == null) {
                    nullJoinpoints.increment();
                    return false;
                }
    
                boolean accepted = jp.instanceOf(type);
                if (!accepted) {
                    excludedJoinpoints.increment();
                }
                return accepted;
            })
            // .filter(jp -> jp.getJoinpointType().equals(type))
            .toArray(AJoinPoint[]::new);
    
    // Count as selected nodes
    getWeaverEngine().getWeavingReport().inc(ReportField.JOIN_POINTS,
            descendants.length + excludedJoinpoints.getCurrent());
    getWeaverEngine().getWeavingReport().inc(ReportField.FILTERED_JOIN_POINTS, descendants.length);
    */

}
