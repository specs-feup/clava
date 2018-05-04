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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.LiteralNode;

public class LiteralType extends Type implements LiteralNode {

    public LiteralType(DataStore nodeData, Collection<? extends ClavaNode> children) {
        super(nodeData, children);
    }

    /*
    public LiteralType(String literalType) {
        this(new LegacyToDataStore()
                .setNodeInfo(ClavaNodeInfo.undefinedInfo())
                .setType(new TypeData(literalType))
                .getData(),
    
                Collections.emptyList());
    
        getDataI().add(LiteralNode.LITERAL_CODE, literalType);
    }
    */
    /*
    private LiteralType(String literalType, ClavaNodeInfo info) {
        super(new TypeData(literalType), info, Collections.emptyList());
    }
    
    @Override
    protected ClavaNode copyPrivate() {
        return new LiteralType(getBareType(), getInfo());
    }
    
    @Override
    public String getCode() {
        return getBareType();
    }
    */
}
