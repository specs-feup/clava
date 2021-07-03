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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AClass;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMethod;

public class CxxMethod extends AMethod {

    private final CXXMethodDecl method;

    public CxxMethod(CXXMethodDecl method) {
        super(new CxxFunction(method));

        this.method = method;
    }

    @Override
    public ClavaNode getNode() {
        return method;
    }

    @Override
    public AClass getRecordImpl() {
        return method.getRecordDecl().map(record -> (AClass) CxxJoinpoints.create(record)).orElse(null);
    }

    @Override
    public void removeRecordImpl() {
        method.removeRecord();
    }

    /**
     * TODO: CxxFunction already overrides this method, this sould not be necessary. Change the WeaverGenerator so that
     * this is not required
     */
    @Override
    public AJoinPoint getTypeImpl() {
        return CxxJoinpoints.create(method.getReturnType());
    }

    /*
    @Override
    public void defRecordImpl(AClass value) {
        method.set(CXXMethodDecl.RECORD, (CXXRecordDecl) value.getNode());
    }
    
    @Override
    public void setRecordImpl(AClass classJp) {
        defRecordImpl(classJp);
    }
    */
}
