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

package pt.up.fe.specs.clava.transform.call;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.ArrayType;
import pt.up.fe.specs.clava.ast.type.ConstantArrayType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.classmap.BiConsumerClassMap;
import pt.up.fe.specs.util.collections.AccumulatorMap;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class InlineRenamer {

    private final CallExpr call;
    private final FunctionDecl functionDecl;
    private final List<Stmt> stmts;
    private final Set<String> usedNames;

    private final BiConsumerClassMap<Expr, ParmVarDecl> argumentsRenamers;

    // Statements that need to be called before the inlined statements
    private final List<Stmt> prefixStmts;
    // private final Map<String, String> renameMap;
    private final Map<String, Consumer<ClavaNode>> renameActions;
    private final Set<String> newNames;
    private Expr callReplacement;

    private AccumulatorMap<String> newNamesCounter;
    private ClavaFactory factory;

    public InlineRenamer(CallExpr call, FunctionDecl functionDecl, List<Stmt> stmts, Set<String> usedNames) {
        this.call = call;
        this.functionDecl = functionDecl;
        this.stmts = stmts;
        this.usedNames = usedNames;

        this.argumentsRenamers = buildArgumentsRenamers();

        this.prefixStmts = new ArrayList<>();
        // this.renameMap = new HashMap<>();
        this.renameActions = new HashMap<>();
        this.newNames = new HashSet<>();
        this.callReplacement = null;

        this.newNamesCounter = new AccumulatorMap<>();

        // Store reference to factory
        this.factory = call.getFactory();
    }

    private BiConsumerClassMap<Expr, ParmVarDecl> buildArgumentsRenamers() {
        BiConsumerClassMap<Expr, ParmVarDecl> argumentsRenamers = new BiConsumerClassMap<>();

        argumentsRenamers.put(DeclRefExpr.class, this::argumentRename);
        argumentsRenamers.put(ArraySubscriptExpr.class, this::argumentRename);
        argumentsRenamers.put(Expr.class, this::argumentRenameGeneric);

        return argumentsRenamers;
    }

    public List<Stmt> getPrefixStmts() {
        return prefixStmts;
    }

    // public Map<String, String> getRenameMap() {
    // return renameMap;
    // }

    public List<Stmt> apply() {
        String calleeName = call.getCalleeName();

        List<ParmVarDecl> parameters = functionDecl.getParameters();
        List<Expr> arguments = call.getArgs();

        // Map declaration names
        for (int i = 0; i < parameters.size(); i++) {
            // If no more arguments, just prefix the name of the call
            if (arguments.size() < i) {
                renameParamWithoutArg(calleeName, parameters.get(i));
                continue;
            }

            argumentRename(arguments.get(i), parameters.get(i));
        }

        // Add VarDecls to rename map
        stmts.stream().flatMap(node -> node.getDescendantsAndSelfStream())
                .filter(VarDecl.class::isInstance)
                .map(VarDecl.class::cast)
                .forEach(varDecl -> renameVarDecl(calleeName, varDecl));

        // Apply renames
        // return applyRenames();
        return applyRenameActions();

    }

    /*
    private List<Stmt> applyRenames() {
        stmts.stream().flatMap(node -> node.getDescendantsAndSelfStream())
                .filter(node -> node instanceof VarDecl || node instanceof DeclRefExpr)
                .forEach(node -> {
                    if (node instanceof VarDecl) {
                        // Check if name is in rename map
                        String declName = ((VarDecl) node).getDeclName();
                        String newName = renameMap.get(declName);
                        if (newName == null) {
                            return;
                        }
    
                        ((VarDecl) node).setDeclName(newName);
                        return;
                    }
    
                    if (node instanceof DeclRefExpr) {
                        // Check if name is in rename map
                        String declName = ((DeclRefExpr) node).getRefName();
                        String newName = renameMap.get(declName);
                        if (newName == null) {
                            return;
                        }
    
                        ((DeclRefExpr) node).setRefName(newName);
                        return;
                    }
    
                    throw new RuntimeException("Not implemented: " + node.getNodeName());
                });
    
        Optional<Stmt> returnReplacement = getReturnReplacement();
    
        // Add return replacement
        returnReplacement.ifPresent(stmts::add);
    
        // Prefix new statements
        return SpecsCollections.concat(prefixStmts, stmts);
    }
    */
    private String getVarName(ClavaNode node) {
        if (node instanceof VarDecl) {
            return ((VarDecl) node).getDeclName();
        }

        if (node instanceof DeclRefExpr) {
            return ((DeclRefExpr) node).getRefName();
        }

        throw new RuntimeException("Not implemented: " + node.getNodeName());
    }

    private void applyRenameAction(ClavaNode node) {
        String varName = getVarName(node);
        Consumer<ClavaNode> action = renameActions.get(varName);
        if (action == null) {
            return;
        }

        action.accept(node);
    }

    private List<Stmt> applyRenameActions() {
        stmts.stream().flatMap(node -> node.getDescendantsAndSelfStream())
                .filter(node -> node instanceof VarDecl || node instanceof DeclRefExpr)
                .forEach(this::applyRenameAction);

        TypeRenamerFilter typeRenamerFilter = new TypeRenamerFilter();

        // AccumulatorMap<String> acc = new AccumulatorMap<>();
        // Also rename DeclRefExpr found in Types
        // Also types might need renaming
        for (Stmt stmt : stmts) {
            for (ClavaNode node : stmt.getDescendantsAndSelf(ClavaNode.class)) {
                // System.out.println("CHECK STMT:" + node.getCode());
                // if (node instanceof VarDecl) {
                // VarDecl varDecl = (VarDecl) node;
                // if (varDecl.getTypeCode().equals("double (*)[x][5]")) {
                //
                // System.out.println("DESCENDANTSSS");
                // varDecl.getType().getTypeDescendantsAndSelfStream()
                // .forEach(type -> {
                // System.out.println("TYPE:" + type);
                // System.out.println("FIELDS:" + type.getNodeFields());
                // });
                // // for (Type type : varDecl.getType().getTypeDescendantsAndSelfStream()) {
                // // System.out.println("TYPE:" + type);
                // // System.out.println("FIELDS:" + type.getNodeFields());
                // // }
                // // System.out.println("FIELDSSS");
                // // for (ClavaNode field : varDecl.getType().getNodeFieldsRecursive()) {
                // // if (field instanceof DeclRefExpr) {
                // // System.out
                // // .println("DECK NAME: " + field.get(DeclRefExpr.DECL).get(ValueDecl.DECL_NAME));
                // // }
                // // System.out.println(field);
                // // }
                // }
                // }

                // if (node instanceof VarDecl) {
                // VarDecl varDecl = (VarDecl) node;
                // if (varDecl.getTypeCode().equals("double (*)[x][5]")) {
                // // System.out.println("VARDECL TYPE:" + varDecl.getType());
                // // System.out.println(
                // // "POINTER TYPE:" + ((VarDecl) node).getType().getChild(Type.class, 0).desugar());
                //
                // System.out.println("FIELDSSDS");
                // for (Type descendant : varDecl.getType().getDescendantsAndSelf(Type.class)) {
                // for (ClavaNode field : descendant.getDesugaredNodeFields()) {
                // System.out.println("FIELD:" + field);
                // }
                // /*
                // Type currentType = descendant;
                // while (currentType != null) {
                // for (ClavaNode desc : currentType.getNodeFields()) {
                // System.out.println(desc);
                // }
                //
                // if (currentType.hasSugar()) {
                // currentType = currentType.desugar();
                // } else {
                // currentType = null;
                // }
                //
                // }
                // */
                // }
                // // for (ClavaNode desc : ((VarDecl) node).getType().getDescendantsAndFields()) {
                // // for (ClavaNode desc : varDecl.getType().getNodeFields()) {
                // // System.out.println(desc);
                // // }
                // // System.out.println(((VarDecl) node).getType().getDescendantsAndFields());
                //
                // // ((VarDecl) node).getType().getDescendantsAndFields().stream().map(Object::toString)
                // // .collect(Collectors.joining("\n"));
                //
                // // System.out.println("TYPE FIELDS:" + ((VarDecl) node).getNodeFieldsRecursive().stream()
                // // .map(Object::toString).collect(Collectors.joining("\n")));
                // }
                // // System.out.println("VARDECL TYPE:" + ((VarDecl) node).getTypeCode());
                // }

                if (!(node instanceof Typable)) {
                    continue;
                }

                Typable typable = (Typable) node;

                // Type candidateType = typable.getType().deepCopy();
                Type candidateType = typable.getType().copy();

                List<ClavaNode> nodesToRename = typeRenamerFilter.nodesToRename(candidateType);

                if (nodesToRename.isEmpty()) {
                    continue;
                }
                // System.out.println("CANDIDATE TYPE: " + candidateType.toFieldTree());

                // System.out.println("NODES IS NOT EMPTY:" + nodesToRename);
                // Set type copy
                typable.setType(candidateType);

                // Apply rename actions
                // System.out.println("TYPE CODE BEFORE:" + candidateType.getCode());
                for (ClavaNode toRename : nodesToRename) {
                    // System.out.println("TO RENAME BEFORE: " + toRename.getCode());
                    applyRenameAction(toRename);
                    // System.out.println("TO RENAME AFTER: " + toRename.getCode());
                }
                // nodesToRename.stream().forEach(this::applyRenameAction);
                // System.out.println("TYPE CODE AFTER:" + candidateType.getCode());
                // boolean hasCandidatesForRenaming = typable.getType().getTypeDescendantsAndSelfStream()
                // .filter(type -> type instanceof VariableArrayType)
                // .findFirst()
                // .isPresent();
                //
                // if (!hasCandidatesForRenaming) {
                // continue;
                // }
                //
                // // typable.getType().getDescendantsNodes().stream().filter(field -> field instanceof DeclRefExpr)
                // // .forEach(field -> System.out.println("Found declref:" + field));
                //
                // // Check if there is a DeclRefExpr descendant
                // // if (typable.getType().getDescendantsAndSelf(DeclRefExpr.class).isEmpty()) {
                // // if (typable.getType().getDescendantsAndFields(DeclRefExpr.class).isEmpty()) {
                // // continue;
                // // }
                // //
                // // boolean hasDeclRefExpr = typable.getType().getDescendantsAndSelfStream()
                // // .filter(Type.class::isInstance)
                // // .map(Type.class::cast)
                // // .flatMap(type -> type.getDesugaredNodeFields().stream())
                // // .filter(DeclRefExpr.class::isInstance)
                // // .findFirst()
                // // .isPresent();
                // //
                // // if (!hasDeclRefExpr) {
                // // continue;
                // // }
                //
                // // There is a DeclRefExpr
                // // Types can be shared among other nodes, copy type before modifying it
                // // Type typeCopy = typable.getType().copyDeep();
                // // Type typeCopy = (Type) typable.getType().deepCopy();
                // Type typeCopy = (Type) typable.getType().deepCopy();
                // typable.setType(typeCopy);
                //
                // System.out.println("CANDIDATEEE " + typable.getType().getTypeDescendantsAndSelfStream().count());
                //
                // // typable.getType().getTypeDescendantsAndSelfStream()
                // typeCopy.getTypeDescendantsAndSelfStream()
                // .forEach(type -> {
                // if (type instanceof VariableArrayType) {
                // // VariableArrayType vat = (VariableArrayType) type;
                // // System.out.println(
                // // "IS COPIED NODE? ID: " + type.get(ClavaNode.ID) + " , PREVIOUS ID: "
                // // + type.get(ClavaNode.PREVIOUS_ID));
                // Expr sizeExpr = type.get(VariableArrayType.SIZE_EXPR);
                //
                // // Create a copy of the DeclRefExpr and set its name
                // if (sizeExpr instanceof DeclRefExpr) {
                // // if (acc.getCount("test") == 6) {
                // // // System.out.println("CURRENT STMTSSS:" + node.getAncestor(Stmt.class));
                // // System.out.println("CURRENT STMTSSS:" + node);
                // // }
                //
                // DeclRefExpr exprCopy = (DeclRefExpr) sizeExpr.copy();
                // exprCopy.set(DeclRefExpr.DECL, (ValueDecl) exprCopy.get(DeclRefExpr.DECL).copy());
                //
                // type.setInPlace(VariableArrayType.SIZE_EXPR, exprCopy);
                // // System.out.println("DECL SIZE EXPR:" + ((DeclRefExpr) sizeExpr).getRefName());
                // System.out.println("RENAMING " + exprCopy.getCode());
                // System.out.println("TYPE BEFORE:" + type.getCode());
                // applyRenameAction(exprCopy);
                // // ((DeclRefExpr) exprCopy).setRefName("test" + acc.add("test"));
                //
                // System.out.println("TYPE AFTER:" + type.getCode());
                // }
                //
                // }
                // // System.out.println("TYPE:" + type);
                // // System.out.println("FIELDS:" + type.getNodeFields());
                // });
                //
                // // System.out.println("TYPES");
                // for (Type descendant : typeCopy.getDescendantsAndSelf(Type.class)) {
                // // System.out.println("DESCENDANT");
                // Type currentType = descendant;
                // while (currentType != null) {
                // // System.out.println("TYPE:" + currentType);
                // // System.out.println("NODE FIELDS:" + descendant.getNodeFields());
                //
                // for (DataKey<?> key : currentType.getKeysWithNodes()) {
                // List<ClavaNode> fields = currentType.getNodes(key);
                //
                // if (fields.size() != 1) {
                // continue;
                // }
                //
                // if (!(fields.get(0) instanceof DeclRefExpr)) {
                // continue;
                // }
                // // System.out.println("SETTING FIELD " + key);
                // DeclRefExpr declRef = (DeclRefExpr) fields.get(0).copy();
                // // declRef.setRefName("xpto");
                // currentType.replaceNodeField(key, Arrays.asList(declRef));
                // // currentType.setInPlace((DataKey<Object>) key, declRef);
                // // System.out.println("GETTING: " + currentType.get(VariableArrayType.SIZE_EXPR).getCode());
                // // List<ClavaNode> fieldsCopy = currentType.copyNodeField(key);
                // // System.out.println("TYPE BEFORE:" + currentType.getCode());
                // // System.out.println("DECLREF BEFORE:" + ((DeclRefExpr) declRef).getRefName());
                // applyRenameAction(declRef);
                // // System.out.println("TYPE AFTER:" + currentType.getCode());
                // // System.out.println("DECLREF AFTER:" + ((DeclRefExpr) declRef).getRefName());
                //
                // }
                //
                // /*
                // for (ClavaNode field : currentType.getNodeFields()) {
                // if (!(field instanceof DeclRefExpr)) {
                // continue;
                // }
                //
                // // Copy declref and queue rename
                //
                // // applyRenameAction(field);
                // System.out.println("DECLREF:" + ((DeclRefExpr) field).getRefName());
                // // System.out.println("DECLREF asdas:" + field);
                // }
                // */
                //
                // Type desugaredType = currentType.desugarTry().orElse(null);
                //
                // // Copy desugared and set it
                // if (desugaredType != null) {
                // desugaredType = desugaredType.copy();
                // currentType.setDesugar(desugaredType);
                // }
                //
                // currentType = desugaredType;
                // }
                // // for (ClavaNode field : descendant.getDesugaredNodeFields()) {
                // // if (!(field instanceof DeclRefExpr)) {
                // // continue;
                // // }
                // //
                // // System.out.println("Renaming FIELD:" + field);
                // // applyRenameAction(field);
                // //
                // // }
                // }

                // Is typable, get all descendants of the type
                // for (DeclRefExpr nodeInType : typeCopy.getDescendantsAndFields(DeclRefExpr.class)) {
                // System.out.println("RENAMING:" + nodeInType);
                // System.out.println("DECLREF:" + nodeInType.getCode());
                // applyRenameAction(nodeInType);
                // // if (nodeInType instanceof DeclRefExpr) {
                // // System.out.println("NODE IN TYPE: " + nodeInType.getCode());
                // //
                // // applyRenameAction(nodeInType);
                // //
                // // }
                // }
            }
        }

        // stmts.stream().flatMap(node -> node.getDescendantsAndSelfStream())
        // // Find nodes with types
        // .filter(Typable.class::isInstance)
        // .flatMap(typable -> ((Typable) typable).getType().getDescendantsAndSelfStream())
        // .filter(DeclRefExpr.class::isInstance)
        // .forEach(this::applyRenameAction);

        // .forEach(declRefExpr -> System.out.println("Found decl ref:" + declRefExpr));

        // // Add types that might refer to variables
        // if (call.getCalleeName().equals("inputInCast")) {
        //
        //
        // // System.out
        // // .println("COPIED STATEMENTS:" + call.getArgs().get(2).getType().toTree());
        // }

        replaceReturn();
        // Optional<Stmt> returnReplacement = getReturnReplacement();

        // Add return replacement
        // returnReplacement.ifPresent(stmts::add);

        // Prefix new statements
        return SpecsCollections.concat(prefixStmts, stmts);
    }

    private void replaceReturn() {
        // If has a return statement, create temporary name a replace return with assigment to this variable
        Optional<ReturnStmt> returnStmtTry = SpecsCollections.reverseStream(stmts)
                .filter(ReturnStmt.class::isInstance)
                .map(ReturnStmt.class::cast)
                .findFirst();

        if (!returnStmtTry.isPresent()) {
            // return Optional.empty();
            return;
        }

        ReturnStmt returnStmt = returnStmtTry.get();

        // Remove return from list of statements
        int returnIndex = SpecsCollections.reverseIndexStream(stmts)
                .filter(i -> stmts.get(i) == returnStmt)
                .findFirst()
                .getAsInt();

        Optional<Expr> retValueTry = returnStmt.getRetValue();

        // If no return value, just remove return
        if (!retValueTry.isPresent()) {
            stmts.remove(returnIndex);
            return;
            // return Optional.empty();
        }

        Expr retValue = retValueTry.get();

        // Create new name
        String returnVarName = getSimpleName(call.getCalleeName(), "return");

        // Create declaration for this new name
        VarDecl varDecl = factory.varDecl(returnVarName, retValue);
        // VarDecl varDecl = ClavaNodeFactory.varDecl(returnVarName, retValue);

        // Replace return with an DeclStmt to the return expression
        DeclStmt declStmt = factory.declStmt(varDecl);
        // DeclStmt declStmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(varDecl));
        stmts.set(returnIndex, declStmt);

        // Save new name expression in callReplacement
        // callReplacement = ClavaNodeFactory.declRefExpr(returnVarName, retValue.getType());
        callReplacement = factory.declRefExpr(returnVarName, retValue.getType());
        // throw new RuntimeException("Not supported yet when function to inline has a return statement");
    }

    public Optional<Expr> getCallReplacement() {
        return Optional.ofNullable(callReplacement);
    }

    private void argumentRename(Expr expr, ParmVarDecl parmVarDecl) {
        argumentsRenamers.accept(expr, parmVarDecl);
    }

    /**
     * Reference to a variable, rename to the name of the original argument, if different.
     * 
     * @param expr
     * @param varDecl
     */
    private void argumentRename(DeclRefExpr expr, ParmVarDecl varDecl) {
        // If not a pointer or an array, apply generic rename (adds copy before inline)
        Type exprType = expr.getType();
        if (!exprType.isArray() && !Types.isPointer(exprType)) {
            argumentRenameGeneric(expr, varDecl);
            return;
        }

        // Rename parameter to the same name as the argument
        String paramName = varDecl.getDeclName();
        String argName = expr.getRefName();

        // If the name is the same, do nothing
        if (paramName.equals(argName)) {
            return;
        }

        // Rename parameter name to be the same as the argument name
        // renameMap.put(paramName, argName);
        renameActions.put(paramName, node -> simpleRename(node, argName));
    }

    private void argumentRename(ArraySubscriptExpr expr, ParmVarDecl parmVarDecl) {

        ArrayType arrayType = Types.getElement(parmVarDecl.getType(), ArrayType.class)
                .orElseThrow(() -> new RuntimeException(
                        "Expected ArraySubscriptExpr to have an array type: " + parmVarDecl.getType()));

        // Special case: ConstantArrayType
        if (arrayType instanceof ConstantArrayType) {
            // Parameter name becomes argument name
            // E.g., c[0][0] with argument k[1] becomes k[1][0][0]
            // String newName = expr.getCode();
            // addRename(parmVarDecl.getDeclName(), newName);
            // addRenameAction(parmVarDecl.getDeclName(), newName);
            renameActions.put(parmVarDecl.getDeclName(), node -> simpleReplace(node, expr));
            return;
        }

        // Get new name
        String newName = getSimpleName(call.getCalleeName(), parmVarDecl.getDeclName());

        // Add renaming
        // addRename(parmVarDecl.getDeclName(), newName);
        addRenameAction(parmVarDecl.getDeclName(), newName);

        // StringBuilder newStmt = new StringBuilder();

        // Get arity of parameter
        Type paramType = parmVarDecl.getType();
        // System.out.println("PARAM TYPE:" + paramType);
        // System.out.println("ARITY:" + Types.getPointerArity(paramType));
        // System.out.println("ELEMENT TYPE:" + Types.getElement(paramType));

        int pointerArity = Types.getPointerArity(paramType);
        Type elementType = Types.getElement(paramType);

        Type newType = elementType;
        for (int i = 0; i < pointerArity; i++) {
            newType = factory.pointerType(newType);
            // newType = ClavaNodeFactory.pointerType(new TypeData("dummy"), ClavaNodeInfo.undefinedInfo(),
            // newType);
        }

        Expr exprWithCast = factory.cStyleCastExpr(newType, expr);
        // Expr exprWithCast = ClavaNodeFactory.cStyleCastExpr(CastKind.NO_OP, new ExprData(newType),
        // ClavaNodeInfo.undefinedInfo(), expr);

        // System.out.println("NEW TYPE:" + newType.getCode());

        // Add statement with declaration of variable
        // VarDeclData varDeclData = parmVarDecl.getVarDeclData().copy();
        // varDeclData.setInitKind(InitializationStyle.CINIT);

        // VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, newName, newType,
        // parmVarDecl.getDeclData(), ClavaNodeInfo.undefinedInfo(), exprWithCast);

        VarDecl varDecl = parmVarDecl.getFactoryWithNode().varDecl(newName, newType);
        varDecl.set(VarDecl.INIT_STYLE, InitializationStyle.CINIT);
        varDecl.setInit(exprWithCast);

        DeclStmt declStmt = factory.declStmt(varDecl);
        // DeclStmt declStmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(varDecl));
        prefixStmts.add(declStmt);

    }

    private void argumentRenameGeneric(Expr expr, VarDecl varDecl) {

        // Get new name
        String newName = getSimpleName(call.getCalleeName(), varDecl.getDeclName());

        // Add renaming
        // addRename(parmVarDecl.getDeclName(), newName);
        addRenameAction(varDecl.getDeclName(), newName);

        // Add statement with declaration of variable
        // VarDeclData varDeclData = varDecl.getVarDeclData().copy();
        // varDeclData.setInitKind(InitializationStyle.CINIT);

        // Sanitize Vardecl type (e.g., transform arrays to pointers)
        // Type varDeclType = sanitizeVarDeclType(varDecl.getType());
        Type varDeclType = expr.getType().copy();

        // VarDecl newVarDecl = ClavaNodeFactory.varDecl(varDeclData, newName, varDeclType,
        // varDecl.getDeclData(), ClavaNodeInfo.undefinedInfo(), expr);

        VarDecl newVarDecl = varDecl.getFactoryWithNode().varDecl(newName, varDeclType);
        newVarDecl.set(VarDecl.INIT_STYLE, InitializationStyle.CINIT);
        newVarDecl.setInit(expr);

        DeclStmt declStmt = factory.declStmt(newVarDecl);
        // DeclStmt declStmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(newVarDecl));
        prefixStmts.add(declStmt);

        // System.out.println("ARRAY SUB");
        // System.out.println("ARG:" + expr.getCode());
        // System.out.println("PARAM:" + parmVarDecl.getCode());
        // System.out.println("ARG TYPE:" + expr.getType());
        // System.out.println("PARAM TYPE:" + parmVarDecl.getType());
    }

    /*
    private Type sanitizeVarDeclType(Type type) {
        Type sanitizedType = type.copy();
    
        // System.out.println("TYPE BEFORE:" + sanitizedType);
        // Replace all array types with pointer types
        Type currentType = sanitizedType;
        while (currentType != null) {
            if (currentType instanceof ArrayType) {
                // Obtain and detach element type
                Type elementType = ((ArrayType) currentType).getElementType();
                elementType.detach();
    
                // Create pointer type
                PointerType pointerType = ClavaNodeFactory.pointerType(currentType.getTypeData(),
                        ClavaNodeInfo.undefinedInfo(), elementType);
    
                NodeInsertUtils.replace(currentType, pointerType);
    
                currentType = elementType;
                continue;
            }
    
            if (currentType.hasSugar()) {
                currentType = currentType.desugar();
            } else {
                currentType = null;
            }
        }
    
        // System.out.println("TYPE AFTER:" + sanitizedType);
    
        return sanitizedType;
    }
    */
    /**
     * Renames a parameter that has no corresponding argument.
     * 
     * @param parmVarDecl
     * @param calleeName
     */
    private void renameParamWithoutArg(String calleeName, ParmVarDecl parmVarDecl) {
        // Get new name
        String newName = getSimpleName(calleeName, parmVarDecl.getDeclName());

        // Add renaming
        // addRename(parmVarDecl.getDeclName(), newName);
        addRenameAction(parmVarDecl.getDeclName(), newName);

        // Add statement with declaration of variable
        // VarDecl varDecl = ClavaNodeFactory.varDecl(parmVarDecl.getVarDeclData(), newName, parmVarDecl.getType(),
        // parmVarDecl.getDeclData(), ClavaNodeInfo.undefinedInfo(), parmVarDecl.getInit().orElse(null));

        VarDecl varDecl = parmVarDecl.getFactoryWithNode().varDecl(newName, parmVarDecl.getType());
        varDecl.setInit(parmVarDecl.getInit().orElse(null));

        DeclStmt declStmt = factory.declStmt(varDecl);
        // DeclStmt declStmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(varDecl));
        prefixStmts.add(declStmt);
    }

    private void renameVarDecl(String calleeName, VarDecl varDecl) {
        // If already renamed (variable shadowing), ignore
        if (renameActions.containsKey(varDecl.getDeclName())) {
            return;
        }

        // Get new name
        String newName = getSimpleName(calleeName, varDecl.getDeclName());

        // Add renaming
        // addRename(varDecl.getDeclName(), newName);
        addRenameAction(varDecl.getDeclName(), newName);
    }

    /*
    private void addRename(String oldName, String newName) {
        String previousName = renameMap.put(oldName, newName);
        if (previousName != null) {
            throw new RuntimeException("Two variables with the same name inside the function (" + oldName
                    + "), are they in different scopes?");
        }
    
        usedNames.add(newName);
    }
    */

    private void addRenameAction(String oldName, String newName) {
        Consumer<ClavaNode> previousAction = renameActions.put(oldName, node -> simpleRename(node, newName));
        if (previousAction != null) {
            throw new RuntimeException("Two variables with the same name inside the function (" + oldName
                    + "), are they in different scopes? Consider checking if name is already mapped and skip in that case.");
        }
        usedNames.add(newName);
    }

    private void simpleRename(ClavaNode node, String newName) {
        if (node instanceof VarDecl) {
            // Check if name is in rename map
            // String declName = ((VarDecl) node).getDeclName();
            // String newName = renameMap.get(declName);
            // if (newName == null) {
            // return;
            // }

            ((VarDecl) node).setDeclName(newName);
            return;
        }

        if (node instanceof DeclRefExpr) {
            // Check if name is in rename map
            // String declName = ((DeclRefExpr) node).getRefName();
            // String newName = renameMap.get(declName);
            // if (newName == null) {
            // return;
            // }

            ((DeclRefExpr) node).setRefName(newName);
            return;
        }

        throw new RuntimeException("Not implemented: " + node.getNodeName());
    }

    private void simpleReplace(ClavaNode node, Expr newExpr) {
        NodeInsertUtils.replace(node, newExpr.copy());
    }

    private String getSimpleName(String calleeName, String declName) {
        String newName = calleeName + "_" + declName;

        // If new name is valid, just return
        if (isNewNameValid(newName)) {
            return newName;
        }

        // Try creating a new name
        int id = newNamesCounter.getCount(newName);
        String newNameBuilder = newName + "_" + id;

        while (!isNewNameValid(newNameBuilder)) {
            id = newNamesCounter.add(newName);
            newNameBuilder = newName + "_" + id;
        }

        return newNameBuilder;
    }

    private boolean isNewNameValid(String newName) {
        if (newNames.contains(newName)) {
            return false;
        }

        if (usedNames.contains(newName)) {
            return false;
        }

        return true;
    }
}
