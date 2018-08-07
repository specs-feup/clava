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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CXXDestructorDecl extends CXXMethodDecl {

    public CXXDestructorDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /*
    public CXXDestructorDecl(String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info, List<ParmVarDecl> inputs,
            Stmt definition) {
    
        super(declName, functionType, functionDeclData, declData, info, inputs, definition);
    }
    
    // protected CXXDestructorDecl(CXXMethodDeclData methodData, String declName, Type functionType,
    protected CXXDestructorDecl(String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {
    
        // super(methodData, declName, functionType, functionDeclData, declData, info, children);
        super(declName, functionType, functionDeclData, declData, info, children);
    }
    
    @Override
    protected ClavaNode copyPrivate() {
    
        return new CXXDestructorDecl(getDeclName(), getFunctionType(), getFunctionDeclData(),
                getDeclData(), getInfo(), Collections.emptyList());
    }
    */

    @Override
    public String getCode() {
        return getCode(false);
    }

}
