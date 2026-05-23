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

import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AClass;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMethod;

public class CxxClass<Self extends CxxClass<Self>> extends AClass<Self> {

    public CxxClass(CXXRecordDecl cxxRecordDecl, CxxWeaver weaver) {
        super(cxxRecordDecl, weaver);
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
    public CXXRecordDecl getNodeImpl() {
        return (CXXRecordDecl) super.getNodeImpl();
    }

    @Override
    public AMethod<?>[] getMethodsImpl() {
        return CxxSelects.select(getWeaverEngine(), AMethod.class, this.getNodeImpl().getMethods(), false, node -> true);
    }

    @Override
    public void addMethodImpl(AMethod<?> method) {
        this.getNodeImpl().addMethod((CXXMethodDecl) method.getNodeImpl());
    }

    @Override
    public AClass<?>[] getBasesImpl() {

        return this.getNodeImpl().getBases().stream()
                .map(decl -> CxxJoinpoints.create(decl,
                        getWeaverEngine(), AClass.class))
                // Collect to array
                .toArray(size -> new AClass[size]);
    }

    @Override
    public AMethod<?>[] getAllMethodsImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getAllMethods(false), getWeaverEngine(), AMethod.class);
    }

    @Override
    public AClass<?>[] getAllBasesImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getAllBases(), getWeaverEngine(), AClass.class);
    }

    @Override
    public boolean getIsAbstractImpl() {
        return this.getNodeImpl().isAbstract();
    }

    @Override
    public boolean getIsInterfaceImpl() {
        return this.getNodeImpl().isInterface();
    }

    @Override
    public AClass<?>[] getPrototypesImpl() {
        return this.getNodeImpl().getDeclarations().stream()
                .map(node -> CxxJoinpoints.create(node,
                        getWeaverEngine(), AClass.class))
                .toArray(AClass[]::new);
    }

    @Override
    public AClass<?> getImplementationImpl() {
        return this.getNodeImpl().getDefinition()
                .map(node -> CxxJoinpoints.create(node,
                        getWeaverEngine(), AClass.class))
                .orElse(null);
    }

    @Override
    public AClass<?> getCanonicalImpl() {
        // First, try the implementation
        var implementation = getImplementationImpl();

        if (implementation != null) {
            return implementation;
        }

        // Implementation not found return prototype
        var prototypes = getPrototypesImpl();

        if (prototypes.length == 0) {
            return null;
        }

        return prototypes[0];
    }

    @Override
    public boolean getIsCanonicalImpl() {
        return this.getNodeImpl().equals(getCanonicalImpl().getNodeImpl());
    }

}
