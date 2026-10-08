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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.HashMap;
import java.util.Map;

import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;

public class CxxVardecl<Self extends CxxVardecl<Self>> extends AVardecl<Self> {

    private static final Lazy<Map<pt.up.fe.specs.clava.ast.decl.enums.StorageClass, StorageClass>> STORAGE_TYPE = new ThreadSafeLazy<>(
            () -> buildStorageTypeMap());

    private static Map<pt.up.fe.specs.clava.ast.decl.enums.StorageClass, StorageClass> buildStorageTypeMap() {
        HashMap<pt.up.fe.specs.clava.ast.decl.enums.StorageClass, StorageClass> storageClasses = new HashMap<>();

        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.None, StorageClass.NONE);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Extern, StorageClass.EXTERN);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Static, StorageClass.STATIC);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.PrivateExtern, StorageClass.PRIVATE_EXTERN);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Auto, StorageClass.AUTO);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Register, StorageClass.REGISTER);

        return storageClasses;
    }

    public CxxVardecl(VarDecl varDecl, CxxWeaver weaver) {
        super(varDecl, weaver);
    }

    @Override
    public VarDecl getNodeImpl() {
        return (VarDecl) super.getNodeImpl();
    }

    @Override
    public boolean getHasInitImpl() {
        return this.getNodeImpl().getInit().isPresent();
    }

    @Override
    public AExpression<?> getInitImpl() {
        return this.getNodeImpl().getInit().map(init -> (AExpression<?>) CxxJoinpoints.create(init, getWeaverEngine())).orElse(null);
    }

    @Override
    public void setInitImpl(AExpression<?> init) {
        if (init == null) {
            removeInitImpl(true);
        } else {
            this.getNodeImpl().setInit((Expr) init.getNodeImpl());
        }
    }

    @Override
    public void setInitImpl(String init) {
        if (init == null) {
            removeInitImpl(true);
        }

        this.getNodeImpl().setInit(getWeaverEngine().getFactory().literalExpr(init, this.getNodeImpl().getType()));
    }

    @Override
    public void removeInitImpl(boolean removeConst) {
        this.getNodeImpl().removeInit(removeConst);
    }

    @Override
    public boolean getIsParamImpl() {
        return false;
    }

    @Override
    public StorageClass getStorageClassImpl() {
        var nodeStorageClass = this.getNodeImpl().get(VarDecl.STORAGE_CLASS);
        if (nodeStorageClass == null) {
            throw new RuntimeException("Storage class of variable '" + getNameImpl() + "' is null");
        }

        StorageClass jpStorageClass = STORAGE_TYPE.get().get(nodeStorageClass);
        if (jpStorageClass == null) {
            throw new RuntimeException("Storage class '" + nodeStorageClass + "' of variable '" + getNameImpl()
                    + "' is not supported in the join point model");
        }

        return jpStorageClass;
    }

    @Override
    public void setStorageClassImpl(StorageClass storageClass) {
        var nodeStorageClass = STORAGE_TYPE.get().entrySet().stream()
                .filter(entry -> entry.getValue() == storageClass)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Storage class '" + storageClass + "' is not supported in the join point model"));

        this.getNodeImpl().setStorageClass(nodeStorageClass);
    }

    @Override
    public boolean getIsGlobalImpl() {
        return this.getNodeImpl().get(VarDecl.HAS_GLOBAL_STORAGE);
    }

    @Override
    public String getInitStyleImpl() {
        return this.getNodeImpl().get(VarDecl.INIT_STYLE).getString();
    }

    @Override
    public AVardecl<?> getDefinitionImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getDefinition(), getWeaverEngine(), AVardecl.class);
    }

    @Override
    public AVarref<?> varrefImpl() {
        return AstFactory.varref(getWeaverEngine(), CxxJoinpoints.create(this.getNodeImpl(), getWeaverEngine(), AVardecl.class));
    }

}
