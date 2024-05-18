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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.LabelDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CXXOperatorCallExpr;
import pt.up.fe.specs.clava.ast.expr.CompoundAssignOperator;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.ExprUse;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.CXXForRangeStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.LiteralStmt;
import pt.up.fe.specs.clava.ast.stmt.NullStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.parsing.snippet.SnippetParser;
import pt.up.fe.specs.clava.utils.Nameable;
import pt.up.fe.specs.clava.utils.NodePosition;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;
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
        return toStmtTry(node).orElseThrow(
                () -> new RuntimeException("Case not defined for class '" + node.getClass().getSimpleName() + "'"));
    }

    public static Optional<Stmt> toStmtTry(ClavaNode node) {
        if (node instanceof Stmt) {
            return Optional.of((Stmt) node);
        }

        if (node instanceof Expr) {
            return Optional.of(node.getFactory().exprStmt((Expr) node));
        }

        // TODO: Is this too restrictive?
        if (node instanceof VarDecl || node instanceof LabelDecl) {
            return Optional.of(node.getFactoryWithNode().declStmt((Decl) node));
        }

        if (node instanceof Comment || node instanceof Pragma) {
            return Optional.of(node.getFactoryWithNode().wrapperStmt(node));
        }

        return Optional.empty();
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
            return hint.getFactory().literalExpr(code, type);
        }

        if (hint instanceof Decl) {
            hint.getFactory().literalDecl(code);
        }

        if (hint instanceof Stmt) {
            hint.getFactory().literalStmt(code);
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

        if (currentNode == null) {
            return null;
        }

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

    private final static Set<UnaryOperatorKind> READ_WRITE_UNARY_OPS = EnumSet.of(UnaryOperatorKind.PreInc,
            UnaryOperatorKind.PreDec,
            UnaryOperatorKind.PostInc, UnaryOperatorKind.PostDec);

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

        boolean isAssign = op.getOp() == BinaryOperatorKind.Assign;
        boolean isCompoundAssign = op instanceof CompoundAssignOperator;

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
     * Marks in the CompoundStmt as 'naked' if it was created from a single Stmt instance.
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

        // Create CompoundStmt
        // Location is important, for insertion of text (e.g., TextParser)
        CompoundStmt newStmt = stmt.getFactory()
                .compoundStmt(stmt);

        // Only make a naked CompoundStmt if node is not a LiteralStmt, we don't know how many statement it represents
        // in that case.
        if (!(stmt instanceof LiteralStmt)) {
            newStmt.setNaked(true);
        }

        newStmt.setLocation(stmt.get(ClavaNode.LOCATION));

        return newStmt;

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

    public static Decl normalizeDecl(Decl decl) {
        // Replace Template Specializations with Templace declarations
        if (decl instanceof FunctionDecl) {
            FunctionDecl fDecl = (FunctionDecl) decl;
            return fDecl.get(FunctionDecl.PRIMARY_TEMPLATE_DECL).orElse((fDecl));
        }

        return decl;
    }

    /**
     * By default, returns the next sibling that is not a comment or a pragma.
     *
     * @return the node targeted by this pragma
     */
    public static Optional<ClavaNode> nextNode(ClavaNode node) {
        if (!node.hasParent()) {
            SpecsLogs.msgLib(
                    "[Clava] Tried to access target of pragma without parent. Pragma: '" + node.getCode() + "'");
            return Optional.empty();
        }

        ClavaNode currentNode = node;
        ClavaNode parent = node.getParent();

        // While parent is a wrapper, replace nodes
        while (parent.isWrapper()) {
            currentNode = parent;
            parent = parent.getParent();

            // In case this node is detached
            if (parent == null) {
                return Optional.empty();
            }
        }

        int indexOfPragma = currentNode.indexOfSelf();

        for (int i = indexOfPragma + 1; i < parent.getNumChildren(); i++) {
            ClavaNode sibling = parent.getChild(i);

            // Ignore comments and pragmas
            if (sibling instanceof Comment || sibling instanceof Pragma || sibling instanceof WrapperStmt) {
                continue;
            }

            // Treat literal statements manually
            if (sibling instanceof LiteralStmt) {
                if (isCommentOrPragma(sibling.getCode())) {
                    continue;
                }
            }

            return Optional.of(sibling);
        }

        return Optional.empty();
    }

    private static boolean isCommentOrPragma(String code) {

        // Trim
        String currentCode = code.trim();

        // Check inline comment
        if (currentCode.startsWith("//")) {
            return true;
        }

        // TODO: Multiline not implemented (yet?)
        if (currentCode.startsWith("/*")) {
            throw new RuntimeException("Testing of literal code for multiline comments not finished yet");
        }

        if (currentCode.startsWith("#pragma")) {
            return true;
        }

        return false;
    }

    /**
     * Insert children from a C/C++ token before/after the reference token, when the token is a statement of is further
     * below in the AST.
     *
     * <p>
     * Minimum granularity level of insert before/after is at the statement level.
     *
     * @param target
     * @param position
     * @param from
     */
    public static ClavaNode insertAsStmt(ClavaNode target, String code, NodePosition position) {

        // Check: if inserting before or after, check if target is valid
        if (!isTargetValid(target, position)) {
            ClavaLog.info("Could not insert code " + position.getString() + " location " + target.getLocation());
            return null;
        }

        SnippetParser snippetParser = new SnippetParser(target.getContext());
        ClavaNode realTarget = null;
        switch (position) {
        case BEFORE:
            Stmt beforeNode = snippetParser.parseStmt(code);
            beforeNode.setOrigin(target);
            realTarget = getValidStatement(target, position);
            if (realTarget == null) {
                return null;
            }

            realTarget = getFirstNodeOfTargetRegion(realTarget, beforeNode);

            NodeInsertUtils.insertBefore(realTarget, beforeNode);
            return beforeNode;

        case AFTER:

            Stmt afterNode = snippetParser.parseStmt(code);
            afterNode.setOrigin(target);
            realTarget = getValidStatement(target, position);
            if (realTarget == null) {
                return null;
            }
            NodeInsertUtils.insertAfter(realTarget, afterNode);
            return afterNode;

        case REPLACE:
            // Has to replace with a node of the same "kind" (e.g., Expr, Stmt...)
            ClavaNode replaceNode = ClavaNodes.toLiteral(code, target.getFactory().nullType(), target);
            replaceNode.setOrigin(target);
            NodeInsertUtils.replace(target, replaceNode);
            return replaceNode;
        default:
            throw new RuntimeException("Case not defined:" + position);
        }
    }

    private static boolean isTargetValid(ClavaNode target, NodePosition position) {

        // If before or after, check if invalid child of a For/ForRange/Do
        if (position == NodePosition.AFTER || position == NodePosition.BEFORE) {
            int indexOfTarget = target.indexOfSelf();
            ClavaNode targetParent = target.getParent();

            // For
            if (targetParent instanceof ForStmt) {
                return indexOfTarget > 2 ? true : false;
            }

            // ForRange
            if (targetParent instanceof CXXForRangeStmt) {
                return indexOfTarget > 4 ? true : false;
            }

            // ForRange
            if (targetParent instanceof DoStmt) {
                return indexOfTarget == 0 ? true : false;
            }
        }

        return true;
    }

    /**
     * Returns the first valid statement where we can insert another node in the after/before inserts
     *
     * @param node
     * @return
     */
    public static Stmt getValidStatement(ClavaNode node, NodePosition position) {
        Stmt target = getValidStatement(node);

        // Check: if inserting before or after, check if target is valid
        if (!isTargetValid(target, position)) {
            ClavaLog.info("Could not insert code " + position.getString() + " location " + target.getLocation());
            return null;
        }

        return target;
    }

    public static Stmt getValidStatement(ClavaNode node) {
        Optional<Stmt> stmt = ClavaNodes.getStatement(node);

        if (!stmt.isPresent()) {
            throw new RuntimeException("Node does not have a statement ancestor:\n" + node);
        }

        return stmt.get();
    }

    /**
     * 
     * @param node
     * @return the name, if the node has one, or null otherwise
     */
    public static String getName(ClavaNode node) {
        if (node instanceof Nameable) {
            return ((Nameable) node).getName();
        }

        if (node instanceof NamedDecl) {
            return ((NamedDecl) node).getDeclName();
        }

        return null;
    }

    /**
     * Splits the given string into two strings, one with the type and another with the name.
     * 
     * <p>
     * E.g.: <br>
     * "int a" returns ["int", "a"] <br>
     * "int *a" returns ["int *", "a"]
     * 
     * @param typeVarName
     * @return
     */
    public static List<String> splitTypeName(String typeVarname) {
        typeVarname = typeVarname.trim();
        int indexOfSpace = typeVarname.lastIndexOf(' ');
        if (indexOfSpace == -1) {
            throw new RuntimeException("Expected parameter to be a type - varName pair, separated by a space");
        }

        // Check if there are * or &
        int indexOfStar = typeVarname.lastIndexOf('*');
        int indexOfAmpersand = typeVarname.lastIndexOf('&');

        var cutIndex = Math.max(indexOfSpace, Math.max(indexOfStar, indexOfAmpersand));

        String type = typeVarname.substring(0, cutIndex + 1).trim();
        String varName = typeVarname.substring(cutIndex + 1).trim();

        return Arrays.asList(type, varName);
    }

    public static ParmVarDecl toParam(String typeVarname, ClavaNode hint) {
        /*
        typeVarname = typeVarname.trim();
        int indexOfSpace = typeVarname.lastIndexOf(' ');
        if (indexOfSpace == -1) {
            throw new RuntimeException("Expected parameter to be a type - varName pair, separated by a space");
        }
        
        // Check if there are * or &
        int indexOfStar = typeVarname.lastIndexOf('*');
        int indexOfAmpersand = typeVarname.lastIndexOf('&');
        
        var cutIndex = Math.max(indexOfSpace, Math.max(indexOfStar, indexOfAmpersand));
        
        String type = typeVarname.substring(0, cutIndex + 1).trim();
        String varName = typeVarname.substring(cutIndex + 1).trim();
        */

        var typeName = splitTypeName(typeVarname);
        // If hint is also a VarDecl, use the same attributes
        var factory = hint instanceof VarDecl ? hint.getFactoryWithNode() : hint.getFactory();

        return factory.parmVarDecl(typeName.get(1), factory.literalType(typeName.get(0)));
    }

    public static ClavaNode getFirstNodeOfTargetRegion(ClavaNode base, ClavaNode newNode) {

        // Check if newNode is a text element (Comment or Pragma, wrapped or not)
        // If so, there is no problem

        var newTextNode = toTextElement(newNode);
        if (newTextNode.isPresent()) {
            return base;
        }

        // Check nodes before base
        var reversedLeftSiblings = new ArrayList<>(base.getLeftSiblings());
        Collections.reverse(reversedLeftSiblings);
        var currentBase = base;

        for (var sibling : reversedLeftSiblings) {
            var baseTextNode = toTextElement(sibling);

            // If sibling is not a text element, return current base
            if (baseTextNode.isEmpty()) {
                // System.out.println("Not a text element: " + sibling.getCode());
                return currentBase;
            }

            // If sibling is a text node, promote it to base node
            currentBase = sibling;
        }

        // Finished loop without returning, first not is a text element
        return currentBase;
    }

    /**
     * A node accepts pragmas if its parent can have pragmas (e.g. CompoundStmt, TranslationUnit)
     * 
     * @param node
     * @return true if the node accepts pragmas, false otherwise
     */
    public static boolean acceptsPragmas(ClavaNode node) {
        var parent = node.getParent();

        if (parent instanceof CompoundStmt) {
            return true;
        }

        if (parent instanceof TranslationUnit) {
            return true;
        }

        return false;
    }
}
