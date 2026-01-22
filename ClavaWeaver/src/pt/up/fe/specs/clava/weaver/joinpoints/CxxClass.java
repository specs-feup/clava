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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AClass;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMethod;

public class CxxClass extends AClass {

    private final CXXRecordDecl cxxRecordDecl;

    public CxxClass(CXXRecordDecl cxxRecordDecl, CxxWeaver weaver) {
        super(new CxxStruct(cxxRecordDecl, weaver), weaver);

        this.cxxRecordDecl = cxxRecordDecl;
    }
    
    public Boolean isAbstract() {
        return (Boolean) this.getIsAbstract();
        /*
        return this.cxxRecordDecl.getMethods().stream()
                .filter(method -> !(method instanceof CXXDestructorDecl))
                .anyMatch(method -> {/*
                    System.err.println(" -> " + method.getFullyQualifiedName() 
                    + " " + method.get(CXXMethodDecl.IS_VIRTUAL).booleanValue()
                    + " " + method.get(CXXMethodDecl.IS_PURE).booleanValue());
                    /** /
                    // System.err.println(method.getCode());
                    
                    return method.get(CXXMethodDecl.IS_PURE).booleanValue();
                });
        */
    }

    @Override
    public ClavaNode getNode() {
        return cxxRecordDecl;
    }

    @Override
    public AMethod[] getMethodsArrayImpl() {
        return CxxSelects.select(getWeaverEngine(), AMethod.class, cxxRecordDecl.getMethods(), false, node -> true).toArray(new AMethod[0]);
    }

    @Override
    public void addMethodImpl(AMethod method) {
        cxxRecordDecl.addMethod((CXXMethodDecl) method.getNode());
    }

    @Override
    public AClass[] getBasesArrayImpl() {

        return cxxRecordDecl.getBases().stream()
                .map(decl -> CxxJoinpoints.create(decl,
                        getWeaverEngine(), AClass.class))
                // Collect to array
                .toArray(size -> new AClass[size]);

        // return cxxRecordDecl.get(CXXRecordDecl.RECORD_BASES).stream()
        // // Map Decl
        // .map(baseSpec -> CxxJoinpoints.create(baseSpec.getBaseDecl(cxxRecordDecl), AClass.class))
        // // Collect to array
        // .toArray(size -> new AClass[size]);
    }

    @Override
    public AMethod[] getAllMethodsArrayImpl() {
        return CxxJoinpoints.create(cxxRecordDecl.getAllMethods(false), getWeaverEngine(), AMethod.class);
    }

    @Override
    public AClass[] getAllBasesArrayImpl() {
        return CxxJoinpoints.create(cxxRecordDecl.getAllBases(), getWeaverEngine(), AClass.class);
    }

    @Override
    public Boolean getIsAbstractImpl() {
        return cxxRecordDecl.isAbstract();
    }

    @Override
    public Boolean getIsInterfaceImpl() {
        return cxxRecordDecl.isInterface();
    }

    @Override
    public AClass[] getPrototypesArrayImpl() {
        return cxxRecordDecl.getDeclarations().stream()
                .map(node -> CxxJoinpoints.create(node,
                        getWeaverEngine(), AClass.class))
                .toArray(size -> new AClass[size]);
    }

    @Override
    public AClass getImplementationImpl() {
        return cxxRecordDecl.getDefinition()
                .map(node -> CxxJoinpoints.create(node,
                        getWeaverEngine(), AClass.class))
                .orElse(null);
    }

    @Override
    public AClass getCanonicalImpl() {
        // First, try the implementation
        var implementation = getImplementationImpl();

        if (implementation != null) {
            return implementation;
        }

        // Implementation not found return prototype
        var prototypes = getPrototypesArrayImpl();

        if (prototypes.length == 0) {
            return null;
        }

        return prototypes[0];
    }

    @Override
    public Boolean getIsCanonicalImpl() {
        return cxxRecordDecl.equals(getCanonicalImpl().getNode());
    }

}
