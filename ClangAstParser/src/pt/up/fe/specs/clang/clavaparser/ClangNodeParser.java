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

package pt.up.fe.specs.clang.clavaparser;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.ClavaNodesLegacy;
import pt.up.fe.specs.clava.ast.comment.FullComment;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

/**
 * Interface for converting GenericClangNodes to ClavaNodes.
 *
 * @author JoaoBispo
 *
 */
public interface ClangNodeParser<T extends ClavaNode> {

    // ClavaNode parse(ClangNode node);
    T parse(ClangNode node);

    // DataStore getDumperData();

    default List<ClavaNode> parseChildren(ClangNode node) {
        return parseChildren(node.getChildrenStream());
    }

    default List<ClavaNode> parseChildren(Stream<ClangNode> stream) {
        return parseChildren(stream, getClass().getSimpleName());
    }

    default List<ClavaNode> parseChildren(Stream<ClangNode> stream, String parentNodeName) {
        // Check if inside a Type parser
        boolean isTypeParser = parentNodeName.endsWith("TypeParser");

        return parseChildren(stream, parentNodeName, isTypeParser);
    }

    default List<ClavaNode> parseChildren(Stream<ClangNode> stream, String parentNodeName, boolean isTypeParser) {

        // boolean isTypeParser = getClass().getSimpleName().endsWith("TypeParser");
        // System.out.println("IS TYPE PARSER:" + isTypeParser);
        return stream.map(child -> parseChild(child, isTypeParser))
                // Remove full comment nodes
                .filter(clavaNode -> !(clavaNode instanceof FullComment))
                .collect(Collectors.toList());

    }

    ClavaNode parseChild(ClangNode node, boolean isTypeParser);

    default <P extends ClavaNode> List<P> parseChildren(List<ClangNode> children, ClangNodeParser<P> parser) {
        return children.stream()
                .map(clangNode -> parser.parse(clangNode))
                .collect(Collectors.toList());
    }

    ClangConverterTable getConverter();

    /**
     * Casts the given node to a Stmt. If not possible, returns a DummyStmt.
     *
     * <p>
     * 1) If the node is of type Expr, encapsulates the node inside a ExprStmt. <br>
     * 2) If the node is of type NullNode, returns null<br>
     * 3) If the node is an Undefined, transforms it into a DummyStmt.
     *
     * @param clavaNode
     * @return
     */
    static Stmt toStmt(ClavaNode node) {
        // If node is an Expr, create statement
        if (node instanceof Expr) {
            return ClavaNodesLegacy.exprStmt((Expr) node);
        }

        if (node instanceof NullNode) {
            // return ClavaNodeFactory.nullStmt(node.getInfo());
            return null;
        }

        return ClavaParserUtils.cast(node, Stmt.class, ClavaNodeFactory::dummyStmt);
    }

    /**
     * Casts the given node to a Stmt that does not uses semicolon. If not possible, throws an exception.
     * 
     * @param node
     * @return
     */
    static Stmt toConditionStmt(ClavaNode node) {

        if (node instanceof Stmt) {
            return (Stmt) node;
        }

        if (node instanceof Expr) {
            return ClavaNodesLegacy.exprStmt(false, (Expr) node);
        }

        if (node instanceof NamedDecl) {
            return ClavaNodeFactory.declStmtWithoutSemicolon(ClavaNodeInfo.undefinedInfo(), (NamedDecl) node);
        }

        throw new RuntimeException("Case not implemented yet:" + node.getClass());
    }
}
