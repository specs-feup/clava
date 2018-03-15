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

package pt.up.fe.specs.clava.ast.decl.data;

import java.util.Optional;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.language.TLSKind;

public class VarDeclData {

    public StorageClass getStorageClass() {
        return storageClass;
    }

    public TLSKind getTlsKind() {
        return tlsKind;
    }

    public boolean isModulePrivate() {
        return isModulePrivate;
    }

    private final StorageClass storageClass;
    private final TLSKind tlsKind;

    private final boolean isModulePrivate;
    private final boolean isNrvo;

    private InitializationStyle initKind;

    private final VarDeclDumperInfo varDeclDumperInfo;

    public VarDeclData() {
        this(StorageClass.NONE, TLSKind.NONE, false, false, InitializationStyle.NO_INIT, false);
    }

    public VarDeclData(StorageClass storageClass, TLSKind tlsKind, boolean isModulePrivate, boolean isNrvo,
            InitializationStyle initKind, VarDeclDumperInfo varDeclDumperInfo) {

        this.storageClass = storageClass;
        this.tlsKind = tlsKind;
        this.isModulePrivate = isModulePrivate;
        this.isNrvo = isNrvo;
        this.initKind = initKind;
        this.varDeclDumperInfo = varDeclDumperInfo;
    }

    /**
     * @deprecated Use construtor that accepts VarDeclDumperInfo, or a future constructor that is simpler
     * @param storageClass
     * @param tlsKind
     * @param isModulePrivate
     * @param isNrvo
     * @param initKind
     * @param isConstexpr
     */
    @Deprecated
    public VarDeclData(StorageClass storageClass, TLSKind tlsKind, boolean isModulePrivate, boolean isNrvo,
            InitializationStyle initKind, boolean isConstexpr) {

        Preconditions.checkArgument(isConstexpr == false, "This constructor only works if 'isConstexpr' is false");

        this.storageClass = storageClass;
        this.tlsKind = tlsKind;
        this.isModulePrivate = isModulePrivate;
        this.isNrvo = isNrvo;
        this.initKind = initKind;
        this.varDeclDumperInfo = new VarDeclDumperInfo(null, false, false, false, false);
    }

    public VarDeclData copy() {
        return new VarDeclData(storageClass, tlsKind, isModulePrivate, isNrvo, initKind, varDeclDumperInfo);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("StorageClass:").append(storageClass);
        string.append(", TLSKind:").append(tlsKind);
        string.append(", isModulePrivate:").append(isModulePrivate);
        string.append(", isNrvo:").append(isNrvo);
        string.append(", init:").append(initKind);
        string.append(", vardecl dumper info:").append(varDeclDumperInfo);

        return string.toString();
    }

    public InitializationStyle getInitKind() {
        return initKind;
    }

    // public boolean isConstexpr() {
    // return isConstexpr;
    // }

    public void setInitKind(InitializationStyle initKind) {
        this.initKind = initKind;
    }

    public boolean isNrvo() {
        return isNrvo;
    }

    public Optional<VarDeclDumperInfo> getVarDeclDumperInfo() {
        return Optional.ofNullable(varDeclDumperInfo);
    }

    public boolean hasVarDeclDumperInfo() {
        return varDeclDumperInfo != null;
    }

    public boolean isConstexpr() {
        return getVarDeclDumperInfo().map(data -> data.isConstexpr()).orElse(false);
    }

    public boolean hasGlobalStorage() {
        return getVarDeclDumperInfo().map(data -> data.hasGlobalStorage()).orElse(false);
    }

    public boolean isStaticDataMember() {
        return getVarDeclDumperInfo().map(data -> data.isStaticDataMember()).orElse(false);
    }

    public boolean isOutOfLine() {
        return getVarDeclDumperInfo().map(data -> data.isOutOfLine()).orElse(false);
    }

    public Optional<String> getQualifiedName() {
        return getVarDeclDumperInfo().map(data -> data.getQualifiedName());
    }
}
