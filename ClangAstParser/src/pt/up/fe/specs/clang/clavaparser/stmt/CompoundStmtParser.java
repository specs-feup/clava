/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.clavaparser.stmt;

import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.NullNodeOld;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.stringparser.StringParser;

/**
 * @deprecated Replaced, safe to delete
 * @author JoaoBispo
 *
 */
@Deprecated
public class CompoundStmtParser extends AClangNodeParser<CompoundStmt> {

    public CompoundStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    public CompoundStmt parse(ClangNode node, StringParser parser) {
        // Parse children
        List<ClavaNode> children = parseChildren(node);

        // Transform Expr nodes into ExprStmt nodes

        // Remove NullNodes and make cast remaining children to typeStmt
        List<Stmt> statements = children.stream()
                // Remove NullNodes
                .filter(child -> !(child instanceof NullNodeOld))
                // Transform Undefined nodes into DummyStmt nodes
                // .map(child -> child instanceof Undefined ? ClavaNodeFactory.dummyStmt(child) : child)
                // Transform Expr nodes into ExprStmt nodes
                // .map(child -> child instanceof Expr ? ClavaNodesLegacy.exprStmt((Expr) child) : child)
                .map(child -> child instanceof Expr ? LegacyToDataStore.getFactory().exprStmt((Expr) child)
                        : child)
                // Map all nodes to Stmt
                .map(stmt -> (Stmt) stmt)
                .collect(Collectors.toList());
        // CompoundStmt stmt = LegacyToDataStore.getFactory().compoundStmt(statements);
        return new CompoundStmt(new LegacyToDataStore().setNodeInfo(info(node)).getData(), statements);
        // return ClavaNodeFactory.compoundStmt(info(node), statements);
    }

}
