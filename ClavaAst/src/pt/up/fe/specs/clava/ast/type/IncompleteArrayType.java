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

public class IncompleteArrayType extends ArrayType {

    public IncompleteArrayType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public IncompleteArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info, Type elementType)
    // {
    // this(arrayTypeData, typeData, info, Arrays.asList(elementType));
    // }

    // private IncompleteArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    // super(arrayTypeData, typeData, info, children);
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new IncompleteArrayType(getArrayTypeData(), getTypeData(), getInfo(), Collections.emptyList());
    // }

    // @Override
    // public Type getElementType() {
    // return getChild(Type.class, 0);
    // }

    /*
    @Override
    public String getCode(String name) {
        String nameCode = name == null ? "" : name;
        return getElementType().getCode() + " " + nameCode + "[]";
    }
    */

    @Override
    protected String getArrayCode() {
        return "";
    }
}
