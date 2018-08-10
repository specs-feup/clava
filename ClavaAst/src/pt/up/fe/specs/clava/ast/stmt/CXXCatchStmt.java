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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NullDecl;

public class CXXCatchStmt extends Stmt {

    public CXXCatchStmt(ClavaNodeInfo info, Decl exceptionDecl, CompoundStmt catchBody) {
        this(info, Arrays.asList(exceptionDecl, catchBody));
    }

    private CXXCatchStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXCatchStmt(getInfo(), Collections.emptyList());
    }

    public Decl getExceptionDecl() {
        return getChild(Decl.class, 0);
    }

    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 1);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        String declCode = getExceptionDecl() instanceof NullDecl ? "..." : getExceptionDecl().getCode();

        // if (declCode.startsWith("std::std::")) {
        // Decl exptDecl = getExceptionDecl();
        // LValueReferenceType type = (LValueReferenceType) ((Typable) exptDecl).getType();
        //
        // System.out.println("LVALUE TYPE:" + type.toTree());
        // System.out.println("LVALUE TYPE CODE:" + type.getCode(this));
        // System.out.println("REFERENCEE CODE:" + type.getReferencee().getCode(this));
        // // if (exptDecl instanceof Typable) {
        // // System.out.println("EXPT DECL TYPE:" + .toTree());
        // //
        // // }
        // throw new RuntimeException("STOP");
        // }

        code.append("catch (").append(declCode).append(")").append(getBody().getCode());

        return code.toString();
    }
}
