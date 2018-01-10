/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;

public class CXXConversionDecl extends CXXMethodDecl {

    public CXXConversionDecl(CXXMethodDeclData methodData, String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info, List<ParmVarDecl> inputs,
            Stmt definition) {

        this(methodData, declName, functionType, functionDeclData, declData, info,
                SpecsCollections.concat(inputs, definition));
    }

    private CXXConversionDecl(CXXMethodDeclData methodData, String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(methodData, declName, functionType, functionDeclData, declData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXConversionDecl(getMethodData(), getDeclName(), getFunctionType(), getFunctionDeclData(),
                getDeclData(), getInfo(), Collections.emptyList());
    }

}
