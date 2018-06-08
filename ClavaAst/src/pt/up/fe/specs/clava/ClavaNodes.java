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

package pt.up.fe.specs.clava;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.CXXOperatorCallExpr;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.enums.ExprUse;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.NullStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * Utility class for ClavaNode objects.
 * 
 * @author JoaoBispo
 *
 */
public class ClavaNodes {

    private static final Set<Class<? extends ClavaNode>> IGNORE_NODES;
    static {
        IGNORE_NODES = new HashSet<>();
        IGNORE_NODES.add(ImplicitCastExpr.class);
        IGNORE_NODES.add(ParenExpr.class);
    }

    public static Stmt toStmt(ClavaNode node) {
        if (node instanceof Stmt) {
            return (Stmt) node;
        }

        if (node instanceof Expr) {
            return LegacyToDataStore.getFactory().exprStmt((Expr) node);
            // return ClavaNodesLegacy.exprStmt((Expr) node);
        }

        if (node instanceof VarDecl) {
            return node.getFactoryWithNode().declStmt((VarDecl) node);
            // return ClavaNodeFactory.declStmt(node.getInfo(), Arrays.asList((VarDecl) node));
        }

        if (node instanceof Comment || node instanceof Pragma) {
            return ClavaNodeFactory.wrapperStmt(node.getInfo(), node);
        }

        throw new RuntimeException("Case not defined for class '" + node.getClass().getSimpleName() + "'");
    }

    /**
     * Creates a literal node, taking an hint node as basis.
     * 
     * <p>
     * For instance, if the hint node is a CXXMemberCallExpr, returns a LiteralExpr.
     * 
     * @param code
     * @param hint
     * @return
     */
    public static ClavaNode toLiteral(String code, Type type, ClavaNode hint) {
        if (hint instanceof Expr) {
            return ClavaNodeFactory.literalExpr(code, type);
        }

        if (hint instanceof Decl) {
            return ClavaNodeFactory.literalDecl(code);
        }

        if (hint instanceof Stmt) {
            hint.getFactory().literalStmt(code);
            // return ClavaNodeFactory.literalStmt(code);
        }

        // Default to statement when unknown
        SpecsLogs.msgLib("Creating literalStmt for hint node of type '" + hint.getClass() + "'");
        return hint.getFactory().literalStmt(code);
    }

    /**
     * Returns the first statement that is related to this node. It can be the node itself, or an ancestor.
     * 
     * @param node
     * @return
     */
    public static Optional<Stmt> getStatement(ClavaNode node) {
        if (node instanceof Stmt) {
            return Optional.of((Stmt) node);
        }

        return node.getAncestorTry(Stmt.class);
    }

    /**
     * Returns the nth child, ignoring Comments, Pragmas and WrapperStmts. Starts at 0.
     * 
     * @param number
     * @return
     */
    public static Optional<ClavaNode> getChildTry(ClavaNode node, int number) {
        Iterator<ClavaNode> iterator = node.getChildrenStream().iterator();

        int counter = 0;
        while (iterator.hasNext()) {
            ClavaNode child = iterator.next();

            if (child instanceof WrapperStmt || child instanceof Pragma || child instanceof Comment) {
                continue;
            }

            if (counter == number) {
                return Optional.of(child);
            }

            counter++;
        }

        // Could not find, return and empty Optional
        return Optional.empty();
    }

    public static ClavaNode getChild(ClavaNode node, int number) {
        return getChildTry(node, number).get();
    }

    /**
     * 
     * @return a list of pragmas associated with this node
     */
    public static List<Pragma> getPragmas(ClavaNode node) {
        int selfIndex = node.indexOfSelf();

        if (selfIndex == -1) {
            return Collections.emptyList();
        }

        // Go backwards, from the self index in the list of siblings, and collect all pragmas
        // while the nodes are TextElements
        List<ClavaNode> siblings = node.getParent().getChildren();
        List<Pragma> pragmas = new ArrayList<>();
        for (int i = selfIndex - 1; i >= 0; i--) {
            ClavaNode sibling = siblings.get(i);

            Optional<ClavaNode> textElement = toTextElement(sibling);

            // Stop when the first non-text element is found
            if (!textElement.isPresent()) {
                break;
            }

            textElement.filter(element -> element instanceof Pragma)
                    .ifPresent(pragma -> pragmas.add((Pragma) pragma));
        }

        // Reverse order of pragmas
        Collections.reverse(pragmas);

        return pragmas;
    }

    public static Optional<ClavaNode> toTextElement(ClavaNode node) {
        if (node instanceof Comment || node instanceof Pragma) {
            return Optional.of(node);
        }

        if (node instanceof WrapperStmt) {
            return toTextElement(((WrapperStmt) node).getWrappedNode());
        }

        return Optional.empty();
    }

    /**
     * Ignores certain nodes, such as ImplicitCastExpr.
     * 
     * @return
     */
    public static ClavaNode getParentNormalized(ClavaNode node) {
        ClavaNode currentNode = node.getParent();

        while (IGNORE_NODES.contains(currentNode.getClass())) {
            currentNode = currentNode.getParent();
        }

        return currentNode;
    }

    /**
     * Ignores certain nodes, such as ImplicitCastExpr.
     * 
     * @return
     */
    public static ClavaNode normalize(ClavaNode node) {
        ClavaNode currentNode = node;

        while (IGNORE_NODES.contains(currentNode.getClass())) {
            Preconditions.checkArgument(currentNode.getNumChildren() == 1,
                    "Expected node to have one child:\n" + currentNode);
            currentNode = currentNode.getChild(0);
        }

        return currentNode;
    }

    private final static Set<UnaryOperatorKind> READ_WRITE_UNARY_OPS = EnumSet.of(UnaryOperatorKind.PRE_INC,
            UnaryOperatorKind.PRE_DEC,
            UnaryOperatorKind.POST_INC, UnaryOperatorKind.POST_DEC);

    /**
     * 
     * @return 'read' if the value in the expression is read, 'write' if the value the expression represents is written,
     *         or 'readwrite' if is both read, and written
     */
    public static ExprUse use(Expr node) {

        ClavaNode parent = ClavaNodes.getParentNormalized(node);

        if (parent instanceof UnaryOperator) {
            boolean isPostOrPre = READ_WRITE_UNARY_OPS.contains(((UnaryOperator) parent).getOp());
            return isPostOrPre ? ExprUse.READWRITE : ExprUse.READ;
        }

        if (parent instanceof CXXOperatorCallExpr) {
            return useOperatorCall((CXXOperatorCallExpr) parent, node);
        }

        if (!(parent instanceof BinaryOperator)) {
            return ExprUse.READ;
        }

        BinaryOperator op = (BinaryOperator) parent;

        boolean isAssign = op.getOp() == BinaryOperatorKind.ASSIGN;
        boolean isCompoundAssign = op instanceof CompoundAssignOperator;
        // if (!op.getOp().isAssign()) {
        if (!(isAssign || isCompoundAssign)) {
            return ExprUse.READ;
        }

        // Check if this node is on the left-hand side of the operator
        boolean isOnLhs = op.getLhs().getDescendantsAndSelfStream()
                // Check if it is the same node
                .filter(descendant -> descendant == node)
                .findFirst()
                .isPresent();

        if (!isOnLhs) {
            return ExprUse.READ;
        }

        if (isAssign) {
            return ExprUse.WRITE;
        }

        Preconditions.checkArgument(isCompoundAssign, "Must be compount assignment: " + op);

        return ExprUse.READWRITE;
    }

    private static ExprUse useOperatorCall(CXXOperatorCallExpr parent, Expr node) {

        if (!parent.getCalleeDeclRef().getRefName().equals("operator=")) {
            return ExprUse.READ;
        }

        // Check if node is on the first argument
        return parent.getArgs().get(0).getDescendantsAndSelfStream()
                .filter(descendant -> descendant == node)
                .findFirst()
                .map(expr -> ExprUse.WRITE)
                .orElse(ExprUse.READ);
    }

    /**
     * Creates a CompoundStmt from the statement, unless the statement already is a CompoundStmt in the list. In that
     * case, returns the statement itself.
     * 
     * <p>
     * Marks in the CompoundStmt as 'naked' if it was created from a list of Stmt instances.
     * <p>
     * If given Stmt is null, returns null.
     * 
     * @param statements
     * @return
     */
    public static CompoundStmt toCompoundStmt(Stmt stmt) {

        if (stmt == null) {
            return null;
        }

        // If CompoundStmt, just return it
        if (stmt instanceof CompoundStmt) {
            return (CompoundStmt) stmt;
        }

        // If NullStmt, create empty naked stmt
        if (stmt instanceof NullStmt) {
            return stmt.getFactory().compoundStmt().setNaked(true);
            // return CompoundStmt.newNakedInstance(stmt.getInfo(), Collections.emptyList());
        }

        // Create naked CompoundStmt
        // Location is important, for insertion of text (e.g., TextParser)
        CompoundStmt newStmt = stmt.getFactory()
                .compoundStmt(stmt)
                .setNaked(true);

        newStmt.setLocation(stmt.get(ClavaNode.LOCATION));

        return newStmt;
        // return CompoundStmt.newNakedInstance(ClavaNodeInfo.undefinedInfo(stmt.getLocation()), Arrays.asList(stmt));

    }

    public static String ln() {
        return SpecsIo.getNewline();
    }

    /**
     * Generic method for printing the code of a node with children.
     * 
     * @param message
     * @param node
     * @return
     */
    public static String toCode(String message, ClavaNode node) {
        StringBuilder builder = new StringBuilder();

        builder.append(message);

        // Get code for children
        String body = node.getChildrenStream()
                .map(child -> child.getCode())
                .collect(Collectors.joining(ln()));

        for (String line : StringLines.getLines(body)) {
            builder.append("    ").append(line);
        }

        return builder.toString();
    }

    /**
     * Generates template code for nodes that have not implemented yet getCode().
     * 
     * @param node
     * @return
     */
    public static String toUnimplementedCode(ClavaNode node) {
        // return ClavaNodeUtils.toCode("// " + node.getClass().getSimpleName() + " - should not generate code", node);
        return toCode("NOT IMPLEMENTED: " + node.getClass().getSimpleName(), node);
    }

    /**
     * Indents the current code.
     * 
     * @param tab
     * @param code
     * @return
     */
    public static String indentCode(String tab, String code) {
        StringLines lines = StringLines.newInstance(code);

        return lines.stream().collect(Collectors.joining(ln() + tab, tab, ln()));
    }

    /**
     * Prepares the type string for code writing.
     * 
     * <p>
     * For instance, generally inserts a space after the type, unless it ends with '*'.
     * 
     * @param type
     * @return
     */
    public static String getTypeCode(String type) {
        if (type.startsWith("enum ")) {
            type = type.substring("enum ".length());
        }

        if (type.startsWith("class ")) {
            type = type.substring("class ".length());
        }

        if (type.endsWith("*")) {
            return type;
        }

        return type + " ";
    }

    public static BinaryOperator newAssignment(Expr leftHand, Expr rightHand) {
        return ClavaNodeFactory.binaryOperator(BinaryOperatorKind.ASSIGN, new ExprData(leftHand.getType()),
                ClavaNodeInfo.undefinedInfo(), leftHand, rightHand);
    }

    public static Stmt getNodeOrNullStmt(Stmt node) {
        if (node == null) {
            return ClavaNodeFactory.nullStmt(ClavaNodeInfo.undefinedInfo());
        }

        return node;
    }

}
