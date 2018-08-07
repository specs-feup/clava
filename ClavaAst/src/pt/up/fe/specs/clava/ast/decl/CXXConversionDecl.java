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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CXXConversionDecl extends CXXMethodDecl {

    public CXXConversionDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /*
    public CXXConversionDecl(String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info, List<ParmVarDecl> inputs,
            Stmt definition) {
    
        this(declName, functionType, functionDeclData, declData, info,
                SpecsCollections.concat(inputs, definition));
    }
    
    private CXXConversionDecl(String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {
    
        super(declName, functionType, functionDeclData, declData, info, children);
    }
    
    @Override
    protected ClavaNode copyPrivate() {
        return new CXXConversionDecl(getDeclName(), getFunctionType(), getFunctionDeclData(),
                getDeclData(), getInfo(), Collections.emptyList());
    }
    */

}
