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
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AField;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ARecord;

public class CxxRecord extends ARecord {

    private final RecordDecl recordDecl;

    public CxxRecord(RecordDecl recordDecl) {
        super(new CxxNamedDecl(recordDecl));
        this.recordDecl = recordDecl;
    }

    @Override
    public ClavaNode getNode() {
        return recordDecl;
    }

    @Override
    public List<? extends AField> selectField() {
        return recordDecl.getFields().stream()
                .map(field -> CxxJoinpoints.create(field, AField.class))
                .collect(Collectors.toList());
    }

    @Override
    public AJoinPoint[] getFieldsArrayImpl() {
        return selectField().toArray(new AJoinPoint[0]);
    }

    @Override
    public String getNameImpl() {
        return recordDecl.getDeclName();
    }

    @Override
    public String getKindImpl() {
        return recordDecl.getTagKind().getCode();
    }

    @Override
    public AFunction[] getFunctionsArrayImpl() {
        return recordDecl.getFunctions().stream()
                .map(function -> (AFunction) CxxJoinpoints.create(function))
                .toArray(size -> new AFunction[size]);
    }

    @Override
    public void addFieldImpl(AField field) {
        recordDecl.addField((FieldDecl) field.getNode());
    }
}
