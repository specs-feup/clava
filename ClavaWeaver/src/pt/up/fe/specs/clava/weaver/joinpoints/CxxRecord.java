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

import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AField;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ARecord;

public class CxxRecord<Self extends CxxRecord<Self>> extends ARecord<Self> {

    public CxxRecord(RecordDecl recordDecl, CxxWeaver weaver) {
        super(recordDecl, weaver);
    }

    @Override
    public RecordDecl getNodeImpl() {
        return (RecordDecl) super.getNodeImpl();
    }

    @Override
    public AField<?>[] getFieldsImpl() {
        return this.getNodeImpl().getFields().stream()
                .map(field -> CxxJoinpoints.create(field,
                        getWeaverEngine(), AField.class))
                .collect(Collectors.toList()).toArray(AField<?>[]::new);
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().getDeclName();
    }

    @Override
    public String getKindImpl() {
        return this.getNodeImpl().getTagKind().getCode();
    }

    @Override
    public AFunction<?>[] getFunctionsImpl() {
        return this.getNodeImpl().getFunctions().stream()
                .map(function -> CxxJoinpoints.create(function, getWeaverEngine(), AFunction.class))
                .toArray(AFunction<?>[]::new);
    }

    @Override
    public void addFieldImpl(AField<?> field) {
        this.getNodeImpl().addField((FieldDecl) field.getNodeImpl());
    }

    @Override
    public boolean getIsImplementationImpl() {
        return this.getNodeImpl().isCompleteDefinition();
    }

    @Override
    public boolean getIsPrototypeImpl() {
        return !this.getNodeImpl().isCompleteDefinition();
    }
}
