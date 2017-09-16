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

    public VarDeclData() {
        this(StorageClass.NONE, TLSKind.NONE, false, false, InitializationStyle.NO_INIT);
    }

    public VarDeclData(StorageClass storageClass, TLSKind tlsKind, boolean isModulePrivate, boolean isNrvo,
            InitializationStyle initKind) {

        this.storageClass = storageClass;
        this.tlsKind = tlsKind;
        this.isModulePrivate = isModulePrivate;
        this.isNrvo = isNrvo;
        this.initKind = initKind;
    }

    public VarDeclData copy() {
        return new VarDeclData(storageClass, tlsKind, isModulePrivate, isNrvo, initKind);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("StorageClass:").append(storageClass);
        string.append(", TLSKind:").append(tlsKind);
        string.append(", isModulePrivate:").append(isModulePrivate);
        string.append(", isNrvo:").append(isNrvo);
        string.append(", init:").append(initKind);

        return string.toString();
    }

    public InitializationStyle getInitKind() {
        return initKind;
    }

    public void setInitKind(InitializationStyle initKind) {
        this.initKind = initKind;
    }

    public boolean isNrvo() {
        return isNrvo;
    }

}
