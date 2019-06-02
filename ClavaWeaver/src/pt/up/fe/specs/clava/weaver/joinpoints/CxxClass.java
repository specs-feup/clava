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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AClass;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMethod;

public class CxxClass extends AClass {

    private final CXXRecordDecl cxxRecordDecl;

    public CxxClass(CXXRecordDecl cxxRecordDecl) {
        super(new CxxStruct(cxxRecordDecl));

        this.cxxRecordDecl = cxxRecordDecl;
    }

    @Override
    public ClavaNode getNode() {
        return cxxRecordDecl;
    }

    @Override
    public AMethod[] getMethodsArrayImpl() {
        return selectMethod().toArray(new AMethod[0]);
    }

    @Override
    public List<? extends AMethod> selectMethod() {
        return CxxSelects.select(AMethod.class, cxxRecordDecl.getMethods(), false, node -> true);
    }

}
