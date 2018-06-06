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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.RecordBase;
import pt.up.fe.specs.clava.ast.decl.data.RecordDeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;

public class CXXRecordDecl extends RecordDecl {

    private final List<RecordBase> recordBases;

    public CXXRecordDecl(List<RecordBase> recordBases, RecordDeclData recordDeclData, Type type, DeclData declData,
            ClavaNodeInfo info,
            List<? extends Decl> children) {

        this(recordBases, recordDeclData, type, declData, info, SpecsCollections.cast(children, ClavaNode.class));
    }

    protected CXXRecordDecl(List<RecordBase> recordBases, RecordDeclData recordDeclData, Type type, DeclData declData,
            ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(recordDeclData, type, declData, info, children);

        this.recordBases = recordBases;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXRecordDecl(new ArrayList<>(recordBases), getRecordDeclData().copy(), getType(), getDeclData(),
                getInfo(), Collections.emptyList());
    }

    public List<RecordBase> getRecordBases() {
        return recordBases;
    }

    @Override
    public String toContentString() {
        String bases = getRecordBases().stream()
                .map(base -> base.getTypeCode())
                .collect(Collectors.joining(","));

        bases = bases.isEmpty() ? "" : ", bases{" + bases + "}";

        return super.toContentString() + bases;
    }

    @Override
    public String getCode() {
        String bases = getRecordBases().stream()
                .map(recordBase -> recordBase.getCode())
                .collect(Collectors.joining(", "));

        bases = bases.isEmpty() ? bases : " : " + bases;

        return super.getCode(bases);
    }

    public List<CXXMethodDecl> getMethods() {
        return getChildrenOf(CXXMethodDecl.class);
    }

    public List<CXXMethodDecl> getMethod(String methodName) {
        List<FunctionDecl> functions = getFunction(methodName);

        return functions.stream()
                .filter(CXXMethodDecl.class::isInstance)
                .map(CXXMethodDecl.class::cast)
                .collect(Collectors.toList());
    }

}
