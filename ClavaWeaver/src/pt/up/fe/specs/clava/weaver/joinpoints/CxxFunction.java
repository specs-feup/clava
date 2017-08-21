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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AParam;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

public class CxxFunction extends AFunction {

    private final FunctionDecl function;
    private final ACxxWeaverJoinPoint parent;

    public CxxFunction(FunctionDecl function, ACxxWeaverJoinPoint parent) {
        this.function = function;
        this.parent = parent;
    }

    @Override
    public String getNameImpl() {
        return function.getDeclName();
    }

    @Override
    public List<? extends AScope> selectBody() {
        CxxScope body = getBodyImpl();

        return body == null ? Collections.emptyList() : Arrays.asList(body);

        // if (!function.hasBody()) {
        // return Collections.emptyList();
        // }
        //
        // return Arrays.asList(new CxxScope(function.getBody().get(), this));
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public FunctionDecl getNode() {
        return function;
    }

    @Override
    public AJoinPoint getTypeImpl() {
        return CxxJoinpoints.create(function.getReturnType(), this);
    }

    @Override
    public AJoinPoint getFunctionTypeImpl() {
        return CxxJoinpoints.create(function.getType(), this);
    }

    @Override
    public Boolean getHasDefinitionImpl() {
        return function.hasBody();
    }

    @Override
    public void insertImpl(String position, String code) {
        Stmt literalStmt = ClavaNodeFactory.literalStmt(code);
        insertStmt(literalStmt, position);
    }

    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return CxxActions.insertJpAsStatement(this, node, "after", getWeaverEngine());
    }

    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return CxxActions.insertJpAsStatement(this, node, "before", getWeaverEngine());
    }

    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        if (node.getNode() instanceof LinkageSpecDecl) {
            return CxxActions.insertJp(this, node, "replace", getWeaverEngine());
        }

        return CxxActions.insertJpAsStatement(this, node, "replace", getWeaverEngine());
    }

    private void insertStmt(Stmt newNode, String position) {
        switch (position) {
        case "before":
            NodeInsertUtils.insertBefore(function, newNode);
            break;

        case "after":
            NodeInsertUtils.insertAfter(function, newNode);
            break;

        case "around":
        case "replace":
            NodeInsertUtils.replace(function, newNode);
            break;
        default:
            throw new RuntimeException("Case not defined:" + position);
        }
    }

    @Override
    public List<? extends AParam> selectParam() {
        return function.getParameters().stream()
                .map(param -> CxxJoinpoints.create(param, this, AParam.class))
                .collect(Collectors.toList());
    }

    @Override
    public String declarationImpl(Boolean withReturnType) {
        return function.getDeclarationId(withReturnType);
    }

    @Override
    public CxxScope getBodyImpl() {
        if (!function.hasBody()) {
            return null;
        }

        return (CxxScope) CxxJoinpoints.create(function.getBody().get(), this);
    }

    @Override
    public void cloneImpl(String newName) {

        if (function instanceof CXXMethodDecl) {

            SpecsLogs.msgInfo(
                    "function " + function.getDeclName() + " is a class method, which is not supported yet for clone");
            return;
        }

        // TODO check if the new name clashes with other symbol?

        // make sure to see if we can just copy
        // function.getDefinition().ifPresent(def -> newFunc.addChild(def.copy()));
        Stmt definition = function.getDefinition().map(stmt -> (Stmt) stmt.copy()).orElse(null);

        // make a new function declaration with the new name
        FunctionDecl newFunc = ClavaNodeFactory.functionDecl(newName,
                function.getParameters(),
                (FunctionType) function.getFunctionType().copy(),
                function.getFunctionDeclData(), // check
                function.getDeclData(), // check
                ClavaNodeInfo.undefinedInfo(), // check
                definition);

        // add a prototype at the location of the original prototype

        // add the clone after the original

        NodeInsertUtils.insertAfter(function, newFunc);

        InlineComment comment = ClavaNodeFactory.inlineComment("cloned from " + function.getDeclName(), false,
                ClavaNodeInfo.undefinedInfo());
        // file.addChild(origIndex + 1, comment);
        NodeInsertUtils.insertAfter(function, comment);

        // change the ids of stuff
        newFunc.getDescendantsStream().forEach(n -> n.setInfo(ClavaNodeInfo.undefinedInfo()));
    }

    @Override
    public String[] getParamNamesArrayImpl() {
        return function.getParameters()
                .stream()
                .map(ParmVarDecl::getCode)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public AJoinPoint[] getParamsArrayImpl() {
        return selectParam().toArray(new AJoinPoint[0]);
    }

    @Override
    public void insertReturnImpl(String code) {
        insertReturnImpl(CxxJoinpoints.create(ClavaNodeFactory.literalStmt(code), null));
    }

    @Override
    public void insertReturnImpl(AJoinPoint code) {
        // Does not take into account situations where functions returns in all paths of an if/else.
        // This means it can lead to dead-code, although for C/C++ that does not seem to be problematic.

        // Do not insert if function has no implementation
        if (!function.hasBody()) {
            return;
        }

        List<Stmt> bodyStmts = function.getBody().get().toStatements();

        // Check if it has return statement
        Stmt lastStmt = SpecsCollections.last(bodyStmts);
        ReturnStmt lastReturnStmt = lastStmt instanceof ReturnStmt ? (ReturnStmt) lastStmt : null;

        // Get list of all return statements inside children
        List<ReturnStmt> returnStatements = bodyStmts.stream()
                .flatMap(Stmt::getDescendantsStream)
                .filter(ReturnStmt.class::isInstance)
                .map(ReturnStmt.class::cast)
                .collect(Collectors.toList());

        if (lastReturnStmt != null) {
            returnStatements = SpecsCollections.concat(returnStatements, lastReturnStmt);
        }
        // If there is no return in the body, add at the end of the function
        else {
            getBodyImpl().insertEnd(code);
        }

        for (ReturnStmt returnStmt : returnStatements) {
            ACxxWeaverJoinPoint returnJp = CxxJoinpoints.create(returnStmt, null);
            returnJp.insertBefore(code);
        }

        // // Find return statement in direct children
        // List<ClavaNode> childStmts = getBodyImpl().selectChildStmt().stream()
        // .map(AStatement::getNode)
        // .collect(Collectors.toList());
        //
        // ReturnStmt endReturn = getLastReturnStmt();

        // TODO Auto-generated method stub
        // super.insertReturnImpl(code);
    }

    /**
     * Uses the declaration, without the return type, to identify the function.
     */
    @Override
    public String getIdImpl() {
        return declarationImpl(false);
    }
}
