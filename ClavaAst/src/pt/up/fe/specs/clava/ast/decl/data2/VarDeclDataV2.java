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

package pt.up.fe.specs.clava.ast.decl.data2;

import pt.up.fe.specs.clava.ast.decl.data.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.data.StorageClass;
import pt.up.fe.specs.clava.language.TLSKind;

public class VarDeclDataV2 extends NamedDeclData {

    public static VarDeclDataV2 empty() {
        return new VarDeclDataV2(StorageClass.NONE, TLSKind.NONE, false, false, InitializationStyle.NO_INIT, false,
                false, false, false, NamedDeclData.empty());
    }

    private final StorageClass storageClass;
    private final TLSKind tlsKind;
    private final boolean isModulePrivate;
    private final boolean isNRVOVariable;
    private final InitializationStyle initStyle;
    private final boolean isConstexpr;
    private final boolean isStaticDataMember;
    private final boolean isOutOfLine;
    private final boolean hasGlobalStorage;

    public VarDeclDataV2(StorageClass storageClass, TLSKind tlsKind, boolean isModulePrivate, boolean isNRVOVariable,
            InitializationStyle initStyle, boolean isConstexpr, boolean isStaticDataMember, boolean isOutOfLine,
            boolean hasGlobalStorage, NamedDeclData namedDeclData) {
        super(namedDeclData);

        this.storageClass = storageClass;
        this.tlsKind = tlsKind;
        this.isModulePrivate = isModulePrivate;
        this.isNRVOVariable = isNRVOVariable;
        this.initStyle = initStyle;
        this.isConstexpr = isConstexpr;
        this.isStaticDataMember = isStaticDataMember;
        this.isOutOfLine = isOutOfLine;
        this.hasGlobalStorage = hasGlobalStorage;
    }

    public VarDeclDataV2(VarDeclDataV2 data) {
        this(data.storageClass, data.tlsKind, data.isModulePrivate, data.isNRVOVariable, data.initStyle,
                data.isConstexpr, data.isStaticDataMember, data.isOutOfLine, data.hasGlobalStorage,
                data);
    }

    public StorageClass getStorageClass() {
        return storageClass;
    }

    public TLSKind getTlsKind() {
        return tlsKind;
    }

    public boolean isModulePrivate() {
        return isModulePrivate;
    }

    public boolean isNRVOVariable() {
        return isNRVOVariable;
    }

    public InitializationStyle getInitStyle() {
        return initStyle;
    }

    public boolean isConstexpr() {
        return isConstexpr;
    }

    public boolean isStaticDataMember() {
        return isStaticDataMember;
    }

    public boolean isOutOfLine() {
        return isOutOfLine;
    }

    public boolean hasGlobalStorage() {
        return hasGlobalStorage;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("storageClass: " + storageClass);
        string.append(",tlsKind: " + tlsKind);
        string.append(",isModulePrivate: " + isModulePrivate);
        string.append(",isNRVOVariable: " + isNRVOVariable);
        string.append(",initStyle: " + initStyle);
        string.append(",isConstexpr: " + isConstexpr);
        string.append(",isStaticDataMember: " + isStaticDataMember);
        string.append(",isOutOfLine: " + isOutOfLine);
        string.append(",hasGlobalStorage: " + hasGlobalStorage);

        return toString(super.toString(), string.toString());
    }
}
