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

package pt.up.fe.specs.clava.ast.extra;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.CXXCtorInitializerKind;

public class CXXCtorInitializer extends ClavaNode {

    // private final String fieldName;

    private final CXXCtorInitializerKind kind;
    private final Type initType;
    private final BareDeclData anyMemberData;

    public CXXCtorInitializer(BareDeclData anyMemberData, Type initType, CXXCtorInitializerKind kind,
            ClavaNodeInfo nodeInfo, Expr defaultInit) {
        this(anyMemberData, initType, kind, nodeInfo, Arrays.asList(defaultInit));
    }

    private CXXCtorInitializer(BareDeclData anyMemberData, Type initType, CXXCtorInitializerKind kind,
            ClavaNodeInfo nodeInfo, Collection<? extends ClavaNode> children) {

        super(nodeInfo, children);

        this.anyMemberData = anyMemberData;
        this.initType = initType;
        this.kind = kind;
        // this.fieldName = fieldName;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXCtorInitializer(anyMemberData, initType, kind, getInfo(), Collections.emptyList());
    }

    public CXXCtorInitializerKind getKind() {
        return kind;
    }

    public Expr getInitExpr() {
        return getChild(Expr.class, 0);
    }

    // public String getFieldName() {
    // return fieldName;
    // }

    public CXXConstructorDecl getConstructor() {
        return (CXXConstructorDecl) getParent();
    }

    @Override
    public String getCode() {

        // if (getAncestor(TranslationUnit.class).getFilename().equals("ForceField.cpp")) {
        // System.out.println("KIND:" + kind);
        // System.out.println("INIT EXPR:" + getInitExpr());
        // System.out.println("PARENT:" + getConstructor().getDeclName());
        // }
        // System.out.println("CTOR KIND:" + kind);
        // System.out.println("LOC:" + getParent().getLocation());

        Expr initExpr = getInitExpr();

        // Preconditions.checkArgument(!(defaultInit instanceof CXXDefaultInitExpr),
        // "Expected 'CXXDefaultInitExpr' nodes to have been removed");
        // Skip if default initialization
        // if (defaultInit instanceof CXXDefaultInitExpr) {
        // System.out.println("DEFAULT INIT EXPR: " + defaultInit.getCode());
        // return "";
        // }

        switch (kind) {
        case DELEGATING_INITIALIZER:
            // return initExpr.getCode();
            return getConstructor().getDeclName() + "(" + initExpr.getCode() + ")";
        case ANY_MEMBER_INITIALIZER:
            return anyMemberData.getDeclName() + "(" + initExpr.getCode() + ")";
        case BASE_INITIALIZER:
            return initType.getCode(this) + "()";
        default:
            throw new RuntimeException("Case not implemented: " + kind + " (loc:" + getParent().getLocation() + ")");

        }
        /*
        if (kind == CXXCtorInitializerKind.DELEGATING_INITIALIZER) {
            return initExpr.getCode();
        }
        
        if (kind == CXXCtorInitializerKind.ANY_MEMBER_INITIALIZER) {
            return anyMemberData.getDeclName() + "(" + initExpr.getCode() + ")";
            // System.out.println("ANY MEMBER");
            // System.out.println("BARE DECL:" + anyMemberData);
            // System.out.println("EXPR:" + initExpr.getCode());
        }
        
        throw new RuntimeException("Case not implemented: " + kind);
        */
        /*
        if (initExpr instanceof CXXConstructExpr) {
            if (((CXXConstructExpr) initExpr).getArgs().isEmpty()) {
                System.out.println("EMPTY!!");
                return "";
            }
            String fieldName = anyMemberData.getDeclName();
            System.out.println("FIELD NAME:" + fieldName);
            return ((CXXConstructExpr) initExpr).getCode(fieldName);
        }
        
        // By default, use format <fieldname>(exprCode)
        // return fieldName + "(" + defaultInit.getCode() + ")";
        return anyMemberData.getDeclName() + "(" + initExpr.getCode() + ")";
        */
    }

    @Override
    public String toContentString() {
        return "kind:" + kind + "; initType:" + initType + "; anyMemberData: [" + anyMemberData + "]";
    }

}
