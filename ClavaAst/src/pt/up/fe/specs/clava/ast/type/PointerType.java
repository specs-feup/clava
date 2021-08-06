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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * C99 6.7.5.1 - Pointer Declarators.
 * 
 * @author JBispo
 *
 */
public class PointerType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Type> POINTEE_TYPE = KeyFactory.object("pointeeType", Type.class);

    /// DATAKEYS END

    public PointerType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        String nameWithPointer = name == null ? "*" : "*" + name;

        return getPointeeType().getCode(sourceNode, nameWithPointer);
    }

    @Override
    public Type getPointeeType() {
        return get(POINTEE_TYPE);
    }

    public static boolean isPointerToParenType(Type type) {
        if (!(type instanceof PointerType)) {
            return false;
        }

        PointerType pointerType = (PointerType) type;

        return pointerType.getPointeeType() instanceof ParenType;
    }

    /**
     * 
     * @return the number of levels of this pointer. If the pointee is not a pointer, returns 1.
     */
    public int getPointerLevels() {
        Type pointee = getPointeeType();
        if (!(pointee instanceof PointerType)) {
            return 1;
        }

        return 1 + ((PointerType) pointee).getPointerLevels();
    }

    @Override
    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        return Arrays.asList(POINTEE_TYPE);
    }

    @Override
    public boolean isPointer() {
        return true;
    }
}
