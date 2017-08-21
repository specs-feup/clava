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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStruct;

public class CxxStruct extends AStruct {

    private final RecordDecl recordDecl;
    private final ACxxWeaverJoinPoint parent;

    public CxxStruct(RecordDecl recordDecl, ACxxWeaverJoinPoint parent) {
        super(new CxxRecord(recordDecl, parent));
        this.recordDecl = recordDecl;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return recordDecl;
    }

    /*
    @Override
    public List<? extends ANamedDecl> selectField() {
        return recordDecl.getFields().stream()
                .map(field -> new CxxNamedDecl(field, this))
                .collect(Collectors.toList());
    }
    */
    /*
    @Override
    public AJoinPoint[] getFieldsArrayImpl() {
        return selectField().toArray(new AJoinPoint[0]);
    }
    
    @Override
    public String getNameImpl() {
        return recordDecl.getDeclName();
    }
    */
}
