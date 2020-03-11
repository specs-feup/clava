/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.analysis.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CXXThrowExpr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.NullStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class ControlFlowGraph {

    private CompoundStmt body;
    private Map<Stmt, BasicBlock> bbs;
    private List<BasicBlockEdge> edges;

    public ControlFlowGraph(FunctionDecl func) {

        this(func.getBody().get()); // exception if func doesn't have body
    }

    public ControlFlowGraph(CompoundStmt body) {

        this.body = body;
        this.bbs = new HashMap<>();
        this.edges = new ArrayList<>();

        findBBs(body);
        fillBBs();
        findBBsTypes();
        findEdges();
    }

    private void findBBsTypes() {

        for (BasicBlock bb : bbs.values()) {

            Stmt last = bb.last();

            if (last instanceof LoopStmt) {

                bb.setType(BasicBlockType.LOOP);
            } else if (last instanceof IfStmt) {

                bb.setType(BasicBlockType.IF);
            } else if (last instanceof ReturnStmt) {

                bb.setType(BasicBlockType.EXIT);
            } else if (last instanceof ExprStmt &&
                    last.getChild(0) instanceof CXXThrowExpr) {

                bb.setType(BasicBlockType.EXIT);
            } else {

                bb.setType(BasicBlockType.NORMAL);
            }
        }
    }

    // TODO: test a loop/if that is the last stmt of the function
    private void findEdges() {

        for (BasicBlock bb : bbs.values()) {

            switch (bb.getType()) {
            case EXIT:
                // no out edges if this is an exit bb
                break;
            case IF:
                IfStmt lastIf = (IfStmt) bb.last();
                // from IfStmt to its Then
                Stmt firstOfThen = (Stmt) lastIf.getThen().get().getChild(0);
                BasicBlock thenBB = bbs.get(firstOfThen);
                bb.addOutEdge(thenBB);
                edges.add(BasicBlockEdge.newTrueEdge(bb, thenBB));
                // from IfStmt to its Else, or SE
                Optional<CompoundStmt> possibleElse = lastIf.getElse();
                if (possibleElse.isPresent()) {
                    Stmt firstOfElse = (Stmt) possibleElse.get().getChild(0);
                    BasicBlock elseBB = bbs.get(firstOfElse);
                    bb.addOutEdge(elseBB);
                    edges.add(BasicBlockEdge.newFalseEdge(bb, elseBB));
                } else {
                    Optional<Stmt> ifSe = getSE(lastIf);
                    if (ifSe.isPresent()) {
                        BasicBlock elseBB = bbs.get(ifSe.get());
                        bb.addOutEdge(elseBB);
                        edges.add(BasicBlockEdge.newFalseEdge(bb, elseBB));
                    } // else: there is no SE so it must mean this is the last stmt of the function
                }

                break;
            case LOOP:
                LoopStmt lastLoop = (LoopStmt) bb.last();
                // from LoopStmt to its Body
                Stmt firstOfBody = (Stmt) lastLoop.getBody().getChild(0);
                BasicBlock bodyBB = bbs.get(firstOfBody);
                bb.addOutEdge(bodyBB);
                edges.add(BasicBlockEdge.newLoopEdge(bb, bodyBB));
                // from LoopStmt to its SE
                Optional<Stmt> loopSe = getSE(lastLoop);
                if (loopSe.isPresent()) {
                    BasicBlock loopSeBB = bbs.get(loopSe.get());
                    bb.addOutEdge(loopSeBB);
                    edges.add(BasicBlockEdge.newNoLoopEdge(bb, loopSeBB));
                }
                break;
            case NORMAL:
                Stmt lastStmt = (Stmt) bb.last();

                // DoWhile follows this BB
                List<ClavaNode> rightSiblings = lastStmt.getRightSiblings();
                if (!rightSiblings.isEmpty()) {
                    ClavaNode clavaNode = rightSiblings.get(0);
                    if (clavaNode instanceof DoStmt) {

                        Stmt firstDo = (Stmt) ((DoStmt) clavaNode).getBody().getChild(0);
                        BasicBlock firstDoBB = bbs.get(firstDo);
                        bb.addOutEdge(firstDoBB);
                        edges.add(BasicBlockEdge.newEdge(bb, firstDoBB));
                        continue;
                    }
                }

                // everything else
                Optional<Stmt> possibleSe = getSE(lastStmt);
                if (possibleSe.isPresent()) {

                    Stmt se = possibleSe.get();

                    BasicBlock seBB = bbs.get(se);
                    bb.addOutEdge(seBB);
                    edges.add(BasicBlockEdge.newEdge(bb, seBB));
                }

                break;
            default:
                throw new RuntimeException(
                        "This BB has type '" + BasicBlockType.UNDEFINED + "' and that shouldn't happen. BB info: \n"
                                + bb.toString());
            }
        }
    }

    /**
     * Gets the SE, or Sibling Equivalent. This is either the first right sibling, or the equivalent for CFG purposes,
     * e.g., a scoped statement that is the parent of the stmt.
     * 
     * @param stmt
     * @return Some Stmt if the SE is found, Empty if it is not (end of function?)
     */
    private static Optional<Stmt> getSE(Stmt stmt) {

        List<ClavaNode> siblings = stmt.getRightSiblings();

        if (!siblings.isEmpty()) {

            return Optional.of((Stmt) siblings.get(0));
        } else {

            ClavaNode parentScope = stmt.getParent();
            ClavaNode parent = parentScope.getParent();

            if (parent instanceof FunctionDecl) {
                return Optional.empty();
            }

            Stmt parentStmt = (Stmt) parent;

            if (parentStmt instanceof LoopStmt) {

                return Optional.of(parentStmt);
            } else {

                return getSE(parentStmt);
            }
        }
    }

    private void fillBBs() {

        Set<Stmt> leaderSet = bbs.keySet();
        BasicBlock current = null;

        for (Stmt stmt : body.getDescendants(Stmt.class)) {

            // skip compound stmts and null stmts
            if (stmt instanceof CompoundStmt || stmt instanceof NullStmt) {
                continue;
            }

            // skip statements that are inside loop headers
            if (stmt.getParent() instanceof LoopStmt) {
                continue;
            }

            if (leaderSet.contains(stmt)) {

                current = bbs.get(stmt);
            } else {

                current.addStmt(stmt);
            }
        }
    }

    private BasicBlock addToMap(Stmt stmt) {

        if (!bbs.containsKey(stmt)) {

            String id = "BB" + bbs.size();
            bbs.put(stmt, new BasicBlock(id, stmt));
        }

        return bbs.get(stmt);
    }

    private void findBBs(CompoundStmt body) {

        for (Stmt stmt : body.getChildren(Stmt.class)) {

            // 1) first stmt of the scope
            if (body.getChild(0).equals(stmt)) {

                addToMap(stmt);
            }

            if (stmt instanceof IfStmt) {

                // 2) targets of a jmp
                CompoundStmt t = ((IfStmt) stmt).getThen().get();
                Stmt tChild = (Stmt) t.getChild(0);
                addToMap(tChild);

                Optional<CompoundStmt> e = ((IfStmt) stmt).getElse();
                if (e.isPresent()) {
                    Stmt eChild = (Stmt) e.get().getChild(0);
                    addToMap(eChild);
                }

                // 3) stmt following a jmp
                List<ClavaNode> ifSiblings = stmt.getRightSiblings();
                if (!ifSiblings.isEmpty()) {

                    Stmt ifSibling = (Stmt) ifSiblings.get(0);
                    addToMap(ifSibling);
                } else {

                    // TODO: check if this is actually needed (isn't the parent loop always a candidate?
                    // if it has no siblings, it's either the last stmt
                    // of the function or the last stmt of an intermediate scope
                    Optional<ClavaNode> possibleParentLoop = stmt.getAscendantsStream()
                            .filter(a -> a instanceof LoopStmt)
                            .findFirst();

                    if (possibleParentLoop.isPresent()) {
                        Stmt parentLoop = (Stmt) possibleParentLoop.get();
                        addToMap(parentLoop);
                    }
                }

                // recursively get the leaders from the if scope
                findBBs(t);
                if (e.isPresent()) {
                    findBBs(e.get());
                }
            }

            if (stmt instanceof LoopStmt) {

                CompoundStmt b = ((LoopStmt) stmt).getBody();

                // 2) targets of a jmp
                Stmt bChild = (Stmt) b.getChild(0);
                addToMap(stmt);
                addToMap(bChild);

                // 3) stmt following a jmp
                List<ClavaNode> loopSiblings = stmt.getRightSiblings();
                if (!loopSiblings.isEmpty()) {

                    Stmt loopSibling = (Stmt) loopSiblings.get(0);
                    addToMap(loopSibling);
                } else {

                    // TODO: check if this is actually needed (isn't the parent loop always a candidate?
                    // if it has no siblings, it's either the last stmt
                    // of the function or the last stmt of an intermediate scope
                    Optional<ClavaNode> possibleParentLoop = stmt.getAscendantsStream()
                            .filter(a -> a instanceof LoopStmt)
                            .findFirst();

                    if (possibleParentLoop.isPresent()) {
                        Stmt parentLoop = (Stmt) possibleParentLoop.get();
                        addToMap(parentLoop);
                    }
                }

                // recursively get the leaders from the if scope
                findBBs(b);
            }
        }
    }

    public List<BasicBlock> getBasicBlocks() {
        return new ArrayList<BasicBlock>(bbs.values());
    }

    public String toDot() {

        String SPACE = " ";
        String NL = "\n";

        StringBuilder builder = new StringBuilder("digraph {");
        builder.append(NL);

        // general options TODO: better way to do this?
        builder.append("    graph [splines=polyline, nodesep=0.5]\n" +
                "    node [shape=box,width=0.2,height=0.2,fontname=Courier,fontsize=10,penwidth=0.5];\n" +
                "    edge [penwidth=0.5,fontname=Courier,fontsize=10,labeljust=c];");
        builder.append(NL);
        builder.append(NL);

        // edges
        for (BasicBlockEdge e : edges) {

            builder.append(e.toDot());
        }
        builder.append(NL);

        // nodes
        for (BasicBlock bb : bbs.values()) {

            builder.append(bb.toDot());
        }
        builder.append(NL);

        // source and sinks
        BasicBlock source = findSource();
        builder.append("{rank=source; ");
        builder.append(source.getId());
        builder.append("}");
        builder.append(NL);

        List<BasicBlock> sinks = findSinks();
        builder.append("{rank=sink;");
        for (BasicBlock s : sinks) {
            builder.append(SPACE);
            builder.append(s.getId());
        }
        builder.append("}");

        // end graph
        builder.append(NL);
        builder.append("}");
        builder.append(NL);

        return builder.toString();
    }

    private BasicBlock findSource() {

        for (BasicBlock bb : bbs.values()) {

            if (!bb.hasInEdges()) {

                return bb;
            }
        }

        throw new RuntimeException("Didn't find a source node for the CFG");
    }

    private List<BasicBlock> findSinks() {

        List<BasicBlock> sinks = new ArrayList<BasicBlock>();

        for (BasicBlock bb : bbs.values()) {

            if (!bb.hasOutEdges()) {

                sinks.add(bb);
            }
        }

        return sinks;
    }
}
